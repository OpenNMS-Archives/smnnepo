#!/bin/sh -

MYDIR=`dirname "$0"`
MYDIR=`cd "$MYDIR"; pwd`
MINION_HOME=`cd "$MYDIR"/..; pwd`

MINION_HOME="@INSTPREFIX@"
USERNAME="admin"
PASSWORD="admin"
OPENNMS=""
BROKER=""
LOCATION=""
RUNAS="root"

CONFFILE="/etc/default/minion"

if [ -f $CONFFILE ]; then
        . $CONFFILE
elif [ -f "/etc/sysconfig/minion" ]; then
        CONFFILE="/etc/sysconfig/minion"
        . $CONFFILE
fi

is_running() {
	"$MINION_HOME"/bin/status >/dev/null 2>&1
}

if is_running; then
	echo "Minion is running."
	exit 0
fi

if [ x$RUNAS != xroot ]; then
	SUDO_PREFIX="sudo -u $RUNAS"
	CHUID="--chuid $RUNAS"
fi

echo -n "Starting Minion: "
if [ -z "$OPENNMS" ] || [ -z "$LOCATION" ]; then
	echo "No configuration"
	exit 0
fi

if [ -z "$KARAF_OPTS" ]; then
	KARAF_OPTS=""; export KARAF_OPTS
fi

"$MINION_HOME"/bin/start >/tmp/minion.log 2>&1
RETVAL="$?"
if [ $RETVAL -eq 0 ]; then
	echo "OK"

	OPENNMS=`echo "$OPENNMS" | sed -e 's,/$,,'`
	SCRIPTDIR=$OPENNMS/minion

	# Make sure that the port here matches the default port from the minion.spec
	if [ -z "$BROKER" ]; then
		echo -n "Configuring root instance: URL: $OPENNMS, Location: $LOCATION: "
		"$MINION_HOME"/bin/client -r 30 -a 8201 "source" "\"$SCRIPTDIR/minion-setup.karaf\"" root "\"$USERNAME\"" "\"$PASSWORD\"" "\"$OPENNMS\"" "\"$LOCATION\"" >/tmp/minion.log 2>&1
		RETVAL="$?"
	else
		echo -n "Configuring root instance: URL: $OPENNMS, Broker: $BROKER, Location: $LOCATION: "
		"$MINION_HOME"/bin/client -r 30 -a 8201 "source" "\"$SCRIPTDIR/minion-setup.karaf\"" root "\"$USERNAME\"" "\"$PASSWORD\"" "\"$OPENNMS\"" "\"$BROKER\"" "\"$LOCATION\"" >/tmp/minion.log 2>&1
		RETVAL="$?"
	fi

	if [ $RETVAL -eq 0 ]; then
		echo "OK"
	else
		echo "FAILED. See /tmp/minion.log for details."
		exit $RETVAL
	fi

	# Sleep to allow the subinstances to start up so we don't run into "command not found" errors
	sleep 10

	# List all of the instances and for each instance, call the minion-setup.karaf script
	# Filter out all of the [, ], and / characters from the admin:list output
	"$MINION_HOME"/bin/admin list | grep '^\[' | sed 's#[][/]# #g' | while read LINE; do

		PORT=`echo "$LINE" | awk '{print $1}'`
		INSTANCE=`echo "$LINE" | awk '{print $6}'`

		# Configure all instances except root
		if [ "$INSTANCE" != "root" ]; then
			echo -n "Configuring subinstance $INSTANCE: "
			if [ -z "$BROKER" ]; then
				"$MINION_HOME"/bin/client -r 10 -a $PORT "source" "\"$SCRIPTDIR/minion-setup.karaf\"" $INSTANCE "\"$USERNAME\"" "\"$PASSWORD\"" "\"$OPENNMS\"" "\"$LOCATION\"" >/tmp/minion.log 2>&1
				RETVAL="$?"
			else
				"$MINION_HOME"/bin/client -r 10 -a $PORT "source" "\"$SCRIPTDIR/minion-setup.karaf\"" $INSTANCE "\"$USERNAME\"" "\"$PASSWORD\"" "\"$OPENNMS\"" "\"$BROKER\"" "\"$LOCATION\"" >/tmp/minion.log 2>&1
				RETVAL="$?"
			fi

			if [ $RETVAL -eq 0 ]; then
				echo "OK"
			else
				echo "FAILED. See /tmp/minion.log for details."
				exit $RETVAL
			fi
		fi
	done

	exit 0

else
	echo "FAILED. See /tmp/minion.log for details."
	exit $RETVAL
fi
