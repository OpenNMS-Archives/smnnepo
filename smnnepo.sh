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

"$SMNNEPO_HOME"/bin/start >/dev/null
RETVAL="$?"
if [ $RETVAL -eq 0 ]; then
        # FIXME We can't start until karaf is ready to run the script need a script that will poll for readiness
	sleep 60
	OPENNMS=`echo "$OPENNMS" | sed -e 's,/$,,'`
	if [ -n "$BROKER" ]; then
		echo -n "configuring container: URL=$OPENNMS, Location=$LOCATION: "
		"$SMNNEPO_HOME"/bin/client "source" "\"$OPENNMS/smnnepo/smnnepo-setup.karaf\"" "\"$USERNAME\"" "\"$PASSWORD\"" "\"$OPENNMS\"" "\"$BROKER\"" "\"$LOCATION\"" >/tmp/smnnepo.log 2>&1
		RETVAL="$?"
	else
		echo -n "configuring container: URL=$OPENNMS, Broker=$BROKER, Location=$LOCATION: "
		"$SMNNEPO_HOME"/bin/client "source" "\"$OPENNMS/smnnepo/smnnepo-setup.karaf\"" "\"$USERNAME\"" "\"$PASSWORD\"" "\"$OPENNMS\"" "\"$LOCATION\"" >/tmp/smnnepo.log 2>&1
		RETVAL="$?"
	fi

	if [ $RETVAL -eq 0 ]; then
		echo "OK"
		exit 0
	else
		echo "FAILED, see /tmp/smnnepo.log for details"
		exit $RETVAL
	fi
else
	echo "FAILED"
	exit $RETVAL
fi
