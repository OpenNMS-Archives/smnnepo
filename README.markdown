This is the repository for distributed experiments with OpenNMS.

Running the Standalone Sampler
==============================

1. Build [OpenNMS master](http://github.com/OpenNMS/opennms.git)
2. Build [SMNnepO](http://github.com/OpenNMS/smnnepo.git)
3. Download and unpack [Karaf](http://karaf.apache.org/index/community/download.html)
4. Run Karaf (bin/karaf)
5. Set the URLs to connect to the running OpenNMS system
		config:edit org.opennms.netmgt.sampler.config
		config:propset opennms.home /Users/ranger/git/opennms-master/target/opennms-1.13.1-SNAPSHOT
		config:propset collectdConfigUrl "http://admin:admin@localhost:8980/opennms/rest/config/Red%20Hat/collection"
		config:propset agentListUrl http://admin:admin@localhost:8980/opennms/rest/config/agents
		config:update
		config:edit org.opennms.netmgt.sampler.config.snmp
		config:propset opennms.home /Users/ranger/git/opennms-master/target/opennms-1.13.1-SNAPSHOT
		config:propset snmpConfigUrl http://admin:admin@localhost:8980/opennms/rest/config/snmp
		config:propset datacollectionFileUrl http://admin:admin@localhost:8980/opennms/rest/config/datacollection
		config:propset datacollectionGroupUrls ""
		config:update
6. Install the SMNnepO feature repository in Karaf
		features:addurl mvn:org.opennms.netmgt.sample/karaf//xml
7. Install Camel, the config, scheduler, and SNMP modules
		features:install -v camel sampler-config sampler-scheduler sampler-snmp
8. Add agents to the ${your_source_dir}/sampler-config/src/test/resources directory (TODO: Make this easier)

