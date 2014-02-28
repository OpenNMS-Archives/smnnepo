package org.opennms.netmgt.sampler.snmp;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.opennms.netmgt.api.sample.support.SingletonBeanFactory;

public class CollectdConfigurationService extends SingletonBeanFactory<CollectdConfiguration> {

	private final ConcurrentHashMap<PackageService, PackageAgentList> m_agentLists = new ConcurrentHashMap<PackageService, PackageAgentList>();

	public List<PackageService> getPackageServiceList(CollectdConfiguration config) {

		PackageService[] svcs = new PackageService[] {
				new PackageService("example1", "SNMP", 300000, "example1"),
				new PackageService("example1", "JMX", 300000, "example1")
		};

		return Arrays.asList(svcs);
	}

	public void setPackageAgentList(PackageAgentList pkgAgentList) {
		m_agentLists.put(pkgAgentList.getPackageService(), pkgAgentList);
	}
}
