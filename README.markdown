This is the repository for distributed experiments with OpenNMS.

Running the Standalone Sampler
==============================

1. Build [OpenNMS master](http://github.com/OpenNMS/opennms.git)
1. Build [SMNnepO](http://github.com/OpenNMS/smnnepo.git)
1. Download and unpack [Karaf](http://karaf.apache.org/index/community/download.html)
1. Run Karaf (bin/karaf)
1. Set the home directory for the general collection configuration and SNMP-specific configuration
		config:edit org.opennms.netmgt.sampler.config
		config:propset opennms.home ${your_source_dir}/sampler-config/src/test/resources
		config:update
		config:edit org.opennms.netmgt.sampler.config.snmp
		config:propset opennms.home ${your_source_dir}/sampler-config-snmp/src/test/resources
		config:update
1. Install the SMNnepO feature repository in Karaf
		features:install mvn:org.opennms.netmgt.sample/karaf//xml
1. Install the config, scheduler, and SNMP modules
		features:install sampler-config sampler-scheduler sampler-snmp
1. Add agents to the ${your_source_dir}/sampler-config/src/test/resources directory (TODO: Make this easier)

