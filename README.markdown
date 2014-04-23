This is the repository for distributed experiments with OpenNMS.

Running the Standalone Sampler
==============================

1. Build [OpenNMS master](http://github.com/OpenNMS/opennms.git).
2. Configure a remote monitoring location by following instructions at [OpenNMS: Remote Polling](http://www.opennms.org/wiki/Remote_Polling).

        * Create a polling package in poller-configuration.xml.
        * Create a package in collectd-configuration.xml.
        * Create a location-def in monitoring-locations.xml for the location you're polling.
          Make sure it has both the "collection-package-name" and "polling-package-name" attributes.

3. Build [SMNnepO](http://github.com/OpenNMS/smnnepo.git).
4. Download and unpack [Karaf 2.3](http://karaf.apache.org/index/community/download.html), version 2.3.3 or higher.
5. Run Karaf (bin/karaf).
6. Set the configuration URLs to connect to the running OpenNMS system.

        config:edit org.opennms.netmgt.sampler.config
        config:propset rest.root http://localhost:8980/opennms/rest
        config:propset location.name "Red Hat"
        config:propset username ranger
        config:propset password pass
        config:update

7. Install and run SMNnepO
       1. Standalone Installation (Collector/Storage in One Karaf)

              features:addurl mvn:org.opennms.netmgt.sample/karaf//xml
              features:install -v sampler-with-file-storage
              
       2. Collector Installation (Collector in Karaf)

              features:addurl mvn:org.opennms.netmgt.sample/karaf//xml
              features:install -v sampler-with-activemq-export
              
       3. Receiver Installation (Storage in OpenNMS)

              In OpenNMS (ssh -p 8101 admin@localhost):
              features:addurl mvn:org.opennms.netmgt.sample/karaf//xml
              features:install -v sample-storage-file sample-receiver-activemq

8. Monitor logs for collections. Check for the existence of a sampler.dat file if you are using file output.
