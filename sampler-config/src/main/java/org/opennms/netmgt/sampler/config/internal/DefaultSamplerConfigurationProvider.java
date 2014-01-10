package org.opennms.netmgt.sampler.config.internal;

import org.opennms.netmgt.dao.api.LocationMonitorDao;
import org.opennms.netmgt.sampler.config.SamplerConfiguration;
import org.opennms.netmgt.sampler.config.SamplerConfigurationProvider;

public class DefaultSamplerConfigurationProvider implements SamplerConfigurationProvider {

	private LocationMonitorDao m_locationMonitorDao;

	@Override
	public SamplerConfiguration getConfigForLocation(String location) {
		return new SamplerConfiguration();
	}

	public void setLocationMonitorDao(LocationMonitorDao locationMonitorDao) {
		m_locationMonitorDao = locationMonitorDao;
	}
	
	

}
