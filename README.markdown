This is the repository for distributed experiments with OpenNMS

Running the Standalone Sampler
==============================

1. Build [OpenNMS master](http://github.com/OpenNMS/opennms.git)
2. Build [SMNnepO](http://github.com/OpenNMS/smnnepo.git)
3. Download and unpack [Karaf](http://karaf.apache.org/index/community/download.html)
4. Run Karaf (bin/karaf)
5. features:install mvn:org.opennms.netmgt.sample/karaf//xml
6. features:install sampler-standalone
7. add-agent -v 2c -c public -r 0 -t 3000 -p 161 172.20.1.107 .1
