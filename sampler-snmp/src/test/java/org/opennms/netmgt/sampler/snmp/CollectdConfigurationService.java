package org.opennms.netmgt.sampler.snmp;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class CollectdConfigurationService {
	
	public static class PackageService {

		private String m_packageName;
		private String m_svcName;
		private int m_millis;
		private String m_filterName;

		public PackageService(String packageName, String svcName, int millis, String filterName) {
			m_packageName = packageName;
			m_svcName = svcName;
			m_millis = millis;
			m_filterName = filterName;
		}

		public String getPackageName() {
			return m_packageName;
		}

		public String getSvcName() {
			return m_svcName;
		}

		public int getMillis() {
			return m_millis;
		}

		public String getFilterName() {
			return m_filterName;
		}

		@Override
		public String toString() {
			return "CollectionService [packageName=" + m_packageName
					+ ", svcName=" + m_svcName + ", millis=" + m_millis
					+ ", filterName=" + m_filterName + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((m_packageName == null) ? 0 : m_packageName.hashCode());
			result = prime * result
					+ ((m_svcName == null) ? 0 : m_svcName.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PackageService other = (PackageService) obj;
			if (m_packageName == null) {
				if (other.m_packageName != null)
					return false;
			} else if (!m_packageName.equals(other.m_packageName))
				return false;
			if (m_svcName == null) {
				if (other.m_svcName != null)
					return false;
			} else if (!m_svcName.equals(other.m_svcName))
				return false;
			return true;
		}
		
		
		


	}
	
	public static class PackageAgentList {
		private PackageService m_packageService;
		private List<ServiceAgent> m_agents;
		
		public PackageAgentList(PackageService packageService, List<ServiceAgent> agents) {
			super();
			m_packageService = packageService;
			m_agents = agents;
		}

		public PackageService getPackageService() {
			return m_packageService;
		}

		public List<ServiceAgent> getAgents() {
			return m_agents;
		}

	}

	private final AtomicReference<CollectdConfiguration> m_config = new AtomicReference<CollectdConfiguration>();
	private final ConcurrentHashMap<PackageService, PackageAgentList> m_agentLists = new ConcurrentHashMap<PackageService, PackageAgentList>();
	
	public void setConfiguration(CollectdConfiguration config) {
		m_config.set(config);
	}
	
	public CollectdConfiguration getConfiguration() {
		return m_config.get();
	}
	
	public List<PackageService> getPackageServiceList() {
		
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