This is the repository for distributed experiments with OpenNMS.

Running the Standalone Sampler
==============================

1. Build [OpenNMS master](http://github.com/OpenNMS/opennms.git).
1. Configure a remote monitoring location by following instructions at [OpenNMS: Remote Polling](http://www.opennms.org/wiki/Remote_Polling).
1. Build [SMNnepO](http://github.com/OpenNMS/smnnepo.git).
1. Download and unpack [Karaf 2.3.X](http://karaf.apache.org/index/community/download.html).
1. Run Karaf (bin/karaf).
1. Set the configuration URLs to connect to the running OpenNMS system.

        config:edit org.opennms.netmgt.sampler.config
        config:propset collectdConfigUrl "http://admin:admin@localhost:8980/opennms/rest/config/Red%20Hat/collection"
        config:propset agentListUrl http://admin:admin@localhost:8980/opennms/rest/config/agents
        config:update
        config:edit org.opennms.netmgt.sampler.config.snmp
        config:propset snmpConfigUrl http://admin:admin@localhost:8980/opennms/rest/config/snmp
        config:propset datacollectionFileUrl http://admin:admin@localhost:8980/opennms/rest/config/datacollection
        config:propset datacollectionGroupUrls ""
        config:update

1. Install the SMNnepO feature repository in Karaf.

        features:addurl mvn:org.opennms.netmgt.sample/karaf//xml

1. Install Camel, the config, scheduler, and SNMP modules.

        features:install -v camel sampler-scheduler sampler-snmp sampler-config

1. Monitor logs for collections.
