This is the repository for distributed experiments with OpenNMS

Running the Standalone Sampler
==============================

1. Build [OpenNMS master](http://github.com/OpenNMS/opennms.git)
2. Build [SMNnepO](http://github.com/OpenNMS/smnnepo.git)
3. Download and unpack [Karaf](http://karaf.apache.org/index/community/download.html)
4. Run Karaf (bin/karaf)
5. features:install mvn:org.opennms.netmgt.sample/karaf//xml
6. features:install sampler-standalone
