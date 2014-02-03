package org.opennms.netmgt.sampler.snmp;

import java.util.concurrent.atomic.AtomicReference;


public class SnmpConfigurationService {
	
	private AtomicReference<SnmpConfiguration> m_config = new AtomicReference<SnmpConfiguration>();
	
	public void setConfiguration(SnmpConfiguration config) {
		m_config.set(config);
	}
	
	public SnmpConfiguration getConfiguration() {
		return m_config.get();
	}
	
	

}
