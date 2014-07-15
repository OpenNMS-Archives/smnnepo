Minion is a next-generation design for data collection (and, in the future,
other scheduled tasks like polling).  It is highly distributable, and extremely
configurable, using Camel and ActiveMQ in the default implementation.

Minion can be used by simply adding a feature URL to a Karaf installation and performing
some simple configuration.  Karaf scripts that perform this setup are provided for both
the OpenNMS server side as well as the Minion client side.

Terms
=====

<dl>
  <dt>Dominion</dt> <dd>The coordination/configuration controller, which runs in Karaf.
  Currently the Dominion Controller only runs in the Karaf embedded into OpenNMS.</dd>
  <dt>Minion</dt> <dd>The remote "client" which performs scheduled tasks and reports back
  to a Dominion controller.</dd>
  <dt>ActiveMQ</dt> <dd>Apache's implementation of JMS: the Java Message Service, used
  for communicating between Minions and the Dominion controller.</dd>
  <dt>SMNnepO</dt> <dd>The code name for the codebase implementing the Dominion,
  Minions, and sampler code.  If you see this, we probably just need to update the
  documentation. ;)  It is likely to refer to the Minion, and not the Dominion
  controller.</dd>
</dl>

Requirements
============

The Dominion controller (the OpenNMS side) has the same requirements of OpenNMS, since
it runs inside OpenNMS's embedded karaf.

The minion should run anywhere Java 7 (or higher) is available.

Installing from RPMs
====================

There are now standalone RPMs available for running Minions with OpenNMS.

Installing the Minion Client RPM
--------------------------------

On the system that should be doing collection, download the "smnnepo" RPM
from http://yum.opennms.org/branches/pjsm-2.0/common/opennms/ and install
it, like so: <code>rpm -Uvh smnnepo-1.13\*.rpm</code>


Installing the Minion Server Components
---------------------------------------

First, you should be running OpenNMS from the <code>pjsm/2.0</code> branch for now.
RPMs are available at http://yum.opennms.org/branches/pjsm-2.0/common/opennms/

You'll need OpenNMS (at least <code>opennms-core</code> and
<code>opennms-webapp-jetty</code>), as well as the Minion webapp from the
same location (<code>opennms-webapp-smnnepo</code>).  Configure OpenNMS for
monitoring locations like you would normally (see below for details), and
then start it up.

Starting the Minion Server in OpenNMS
-------------------------------------

1. Connect to the OpenNMS karaf console: <code>ssh -p 8101 admin@localhost</code>
   <br />(password is 'admin')
2. Run the configuration script: <code>source http://localhost:8980/smnnepo/opennms-setup.karaf</code>

This will update your local feature repository, and then install the
"<code>sample-receiver-activemq</code>" feature, to allow for listening
on an internal ActiveMQ server.

Starting the SMNnepO Client
---------------------------

1. Edit /etc/sysconfig/smnnepo, and put in the URL for the root of your
   OpenNMS server, the name of your monitoring location, and (optionally) the
   URL for your ActiveMQ broker.
2. Start the SMNnepO client: <code>sudo /etc/init.d/smnnepo start</code>

<hr>

Installing from Source
======================

1. Build [OpenNMS pjsm/2.0](https://github.com/OpenNMS/opennms/tree/pjsm/2.0)
2. Build [Minion](http://github.com/OpenNMS/smnnepo.git)
3. From the Minion source build, copy <code>sampler-repo-webapp/target/smnnepo.war</code>
   to your OpenNMS <code>jetty-webapps/</code> directory.

Configuring OpenNMS
-------------------

1. Configure a remote monitoring location in OpenNMS by following instructions at
   [OpenNMS: Remote Polling](http://www.opennms.org/wiki/Remote_Polling).

    * Create a polling package in poller-configuration.xml.
    * Create a package in collectd-configuration.xml.
    * Create a location-def in monitoring-locations.xml for the location you're polling.
      Make sure it has both the "collection-package-name" and "polling-package-name"
      attributes.

2. If you didn't already, copy the sampler .war file from the source build above to your
   <code>$OPENNMS\_HOME/jetty-webapps</code> directory.
3. Restart OpenNMS.

Configuring the Dominion Server in OpenNMS
----------------------------------------

1. Connect to the OpenNMS karaf console: <code>ssh -p 8101 admin@localhost</code>
   <br />(password is 'admin')
2. Run the configuration script: <code>source http://localhost:8980/smnnepo/opennms-setup.karaf</code>

Configuring a Minion Client
---------------------------

1. Download and unpack [Karaf 2.3](http://karaf.apache.org/index/community/download.html),
   version 2.3.4 or higher.
2. Run Karaf (<code>bin/karaf</code>)
3. Run the configuration script:
   <code>source http://opennms-host:8980/smnnepo/smnnepo-setup.karaf [username] [password] [opennms-root] [location-name]</code>

	* username: The OpenNMS username allowed to make ReST calls
	* password: The password for the ReST user
	* opennms-root: The root URL of the OpenNMS server, _not_ including the
	  <code>/opennms</code> part of the URL.  For example, if you normally connect to your
	  OpenNMS web UI at <code>http://www.example.com:8980/opennms/</code>, you would use
	  "<code>http://www.example.com:8980/</code>" as the OpenNMS root.
	* location-name: The name of the location the Minion is running at.  This should match
	  a location in the <code>monitoring-locations.xml</code> file on your OpenNMS server.

Validating Your Minion
======================

All minions need validation.  ;)

If everything connected correctly, you should be able to browse to the OpenNMS minion
console at <code>http://opennms-host:8980/opennms/minion/index.jsp</code>.
