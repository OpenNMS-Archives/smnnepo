The sampler is a next-generation design for data collection (and, in the future,
other scheduled tasks like polling).  It is highly distributable, and extremely
configurable, using Camel and ActiveMQ in the default implementation.

The sampler can be used by simply adding a feature URL to a Karaf installation.
It will either install the sampler code by accessing the public OpenNMS maven
repository, or can be configured to instead pull all necessary code from a
webapp.

Installing the Sampler
======================

Installing from Source
----------------------

1. Build [OpenNMS master](http://github.com/OpenNMS/opennms.git).
2. Build [SMNnepO](http://github.com/OpenNMS/smnnepo.git): <code>./compile.pl install</code>

The finished sampler code will be installed in your maven directory, and a war
file suitable for installation elsewhere will be in the
<code>sampler-repo-webapp/target/</code> directory.

Installing from Binary
----------------------

The <code>opennms-sampler-XXXXX.zip</code> file contains a compiled .war file
for installation in your OpenNMS instance.

Running the Standalone Sampler
==============================

Configuring OpenNMS
-------------------

1. Configure a remote monitoring location in OpenNMS by following instructions at [OpenNMS: Remote Polling](http://www.opennms.org/wiki/Remote_Polling).

        * Create a polling package in poller-configuration.xml.
        * Create a package in collectd-configuration.xml.
        * Create a location-def in monitoring-locations.xml for the location you're polling.
          Make sure it has both the "collection-package-name" and "polling-package-name" attributes.

2. Copy the sampler .war file from the source build above, or the binary distribution, to your <code>$OPENNMS\_HOME/jetty-webapps</code> directory.
3. If you do not already have a custom jetty.xml, copy <code>$OPENNMS\_HOME/etc/examples/jetty.xml</code> to <code>$OPENNMS\_HOME/etc</code>.
4. Add a handler for the sampler-repo inside the &lt;Set name="handlers"&gt; array, before the ROOT servlet:

        <Item>
          <New class="org.eclipse.jetty.webapp.WebAppContext">
            <Set name="War"><SystemProperty name="opennms.home" />/jetty-webapps/sampler-repo-webapp-1.13.2-20140506.195600-1.war</Set>
            <Set name="ContextPath">/sampler-repo</Set>
          </New>
        </Item>

5. Restart OpenNMS.

Configuring and Running the Sampler
-----------------------------------

1. Download and unpack [Karaf 2.3](http://karaf.apache.org/index/community/download.html), version 2.3.4 or higher.
2. Run Karaf (bin/karaf).
3. Set the configuration to be able to find the maven repository in the sampler-repo webapp.

        config:edit org.ops4j.pax.url.mvn
        config:propset org.ops4j.pax.url.mvn.repositories http://localhost:8980/sampler-repo@snapshots@id=opennms-repo
        config:update

4. Set the configuration URLs to connect to the running OpenNMS system.

        config:edit org.opennms.netmgt.sampler.config
        config:propset rest.root http://localhost:8980/opennms/rest
        config:propset location.name "Red Hat"
        config:propset username ranger
        config:propset password pass
        config:update

5. Install and run SMNnepO
       1. Standalone Installation (Collector/Storage in One Karaf)

              features:addurl mvn:org.opennms.netmgt.sample/karaf/1.13.2-SNAPSHOT/xml
              features:install -v sampler-with-file-storage
              
       2. Collector Installation (Collector in Karaf)

              features:addurl mvn:org.opennms.netmgt.sample/karaf/1.13.2-SNAPSHOT/xml
              features:install -v sampler-with-activemq-export
              
       3. Receiver Installation (Storage in OpenNMS)

              In OpenNMS (ssh -p 8101 admin@localhost):
              features:addurl mvn:org.opennms.netmgt.sample/karaf/1.13.2-SNAPSHOT/xml
              features:install -v sample-storage-file sample-receiver-activemq

6. Monitor logs for collections. Check for the existence of a sampler.dat file if you are using file output.
