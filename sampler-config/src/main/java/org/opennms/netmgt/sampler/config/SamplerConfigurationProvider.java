package org.opennms.netmgt.sampler.config;

public interface SamplerConfigurationProvider {
	
	SamplerConfiguration getConfigForLocation(String location);

}

