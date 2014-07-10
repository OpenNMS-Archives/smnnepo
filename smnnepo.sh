#!/bin/sh -

MYDIR=`dirname "$0"`
MYDIR=`cd "$MYDIR"; pwd`
SMNNEPO_HOME=`cd "$MYDIR"/..; pwd`

SMNNEPO_HOME="@INSTPREFIX@"
USERNAME="admin"
PASSWORD="admin"
OPENNMS=""
BROKER=""
LOCATION=""
RUNAS="root"

CONFFILE="/etc/default/smnnepo"

if [ -f $CONFFILE ]; then
        . $CONFFILE
elif [ -f "/etc/sysconfig/smnnepo" ]; then
        CONFFILE="/etc/sysconfig/smnnepo"
        . $CONFFILE
fi

is_running() {
	"$SMNNEPO_HOME"/bin/status >/dev/null 2>&1
}

if is_running; then
	echo "SMNnepO is running."
	exit 0
fi

if [ x$RUNAS != xroot ]; then
	SUDO_PREFIX="sudo -u $RUNAS"
	CHUID="--chuid $RUNAS"
fi

echo -n "Starting SMNnepO: "
if [ -z "$OPENNMS" ] || [ -z "$LOCATION" ]; then
	echo "no configuration"
	exit 0
fi

if [ -z "$KARAF_OPTS" ]; then
	KARAF_OPTS=""; export KARAF_OPTS
fi

"$SMNNEPO_HOME"/bin/start >/dev/null
RETVAL="$?"
if [ $RETVAL -eq 0 ]; then
	echo "OK"

	OPENNMS=`echo "$OPENNMS" | sed -e 's,/$,,'`
	SCRIPTDIR=$OPENNMS/smnnepo
	#SCRIPTDIR=/root/git/smnnepo/sampler-repo-webapp/src/main/webapp

	# Make sure that the port here matches the default port from the smnnepo.spec
	if [ -z "$BROKER" ]; then
		echo -n "Configuring root instance: URL: $OPENNMS, Location: $LOCATION: "
		"$SMNNEPO_HOME"/bin/client -r 30 -a 8201 "source" "\"$SCRIPTDIR/smnnepo-setup.karaf\"" root "\"$USERNAME\"" "\"$PASSWORD\"" "\"$OPENNMS\"" "\"$LOCATION\"" >/tmp/smnnepo.log 2>&1
		RETVAL="$?"
	else
		echo -n "Configuring root instance: URL: $OPENNMS, Broker: $BROKER, Location: $LOCATION: "
		"$SMNNEPO_HOME"/bin/client -r 30 -a 8201 "source" "\"$SCRIPTDIR/smnnepo-setup.karaf\"" root "\"$USERNAME\"" "\"$PASSWORD\"" "\"$OPENNMS\"" "\"$BROKER\"" "\"$LOCATION\"" >/tmp/smnnepo.log 2>&1
		RETVAL="$?"
	fi

	if [ $RETVAL -eq 0 ]; then
		echo "OK"
	else
		echo "FAILED. See /tmp/smnnepo.log for details."
		exit $RETVAL
	fi

	# List all of the instances and for each instance, call the smnnepo-setup.karaf script
	#INSTANCES=`"$SMNNEPO_HOME"/bin/admin list | grep '^\[' | sed 's#^\[\s*\([0-9]*\)\]\s*\[\s*\([0-9]*\)/\([0-9]*\)\s*\]\s*\[\s*\([[:alnum:]]*\)\s*\]\s*\[\([0-9]*\)\s*\]\s*\(.*\)$#\1 \2 \3 \4 \5 \6#'`

	# Filter out all of the [, ], and / characters
	"$SMNNEPO_HOME"/bin/admin list | grep '^\[' | sed 's#[][/]# #g' | while read LINE; do
		echo "$LINE" >/tmp/smnnepo.log 2>&1
		PORT=`echo "$LINE" | awk '{print $1}'`
		INSTANCE=`echo "$LINE" | awk '{print $6}'`

		sleep 10

		# Configure all instances except root
		if [ "$INSTANCE" != "root" ]; then
			echo -n "Configuring subinstance $INSTANCE: "
			if [ -z "$BROKER" ]; then
				"$SMNNEPO_HOME"/bin/client -r 10 -a $PORT "source" "\"$SCRIPTDIR/smnnepo-setup.karaf\"" $INSTANCE "\"$USERNAME\"" "\"$PASSWORD\"" "\"$OPENNMS\"" "\"$LOCATION\"" >/tmp/smnnepo.log 2>&1
				RETVAL="$?"
			else
				"$SMNNEPO_HOME"/bin/client -r 10 -a $PORT "source" "\"$SCRIPTDIR/smnnepo-setup.karaf\"" $INSTANCE "\"$USERNAME\"" "\"$PASSWORD\"" "\"$OPENNMS\"" "\"$BROKER\"" "\"$LOCATION\"" >/tmp/smnnepo.log 2>&1
				RETVAL="$?"
			fi

			if [ $RETVAL -eq 0 ]; then
				echo "OK"
			else
				echo "FAILED. See /tmp/smnnepo.log for details."
				exit $RETVAL
			fi
		fi
	done

	exit 0

else
	echo "FAILED"
	exit $RETVAL
fi
