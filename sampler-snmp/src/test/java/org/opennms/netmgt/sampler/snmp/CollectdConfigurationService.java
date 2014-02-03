package org.opennms.netmgt.sampler.snmp;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class CollectdConfigurationService {
	
	public static class CollectionService {

		private String m_packageName;
		private String m_svcName;
		private int m_millis;
		private String m_filterName;

		public CollectionService(String packageName, String svcName, int millis, String filterName) {
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
		


	}

	private final AtomicReference<CollectdConfiguration> m_config = new AtomicReference<CollectdConfiguration>();
	
	public void setConfiguration(CollectdConfiguration config) {
		m_config.set(config);
	}
	
	public CollectdConfiguration getConfiguration() {
		return m_config.get();
	}
	
	public List<CollectionService> getServiceList() {
		
		CollectionService[] svcs = new CollectionService[] {
				new CollectionService("example1", "SNMP", 300000, "example1"),
				new CollectionService("example1", "JMX", 300000, "example1")
		};
		
		return Arrays.asList(svcs);
		
		
	}
}