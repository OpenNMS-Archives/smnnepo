package org.opennms.netmgt.sampler.config;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.opennms.netmgt.api.sample.Agent.AgentList;
import org.opennms.netmgt.config.collectd.CollectdConfiguration;

public abstract class DataFormatUtils {
	public static JaxbDataFormat collectdConfigurationXml() {
		try {
			JAXBContext context = JAXBContext.newInstance(CollectdConfiguration.class);
			return new JaxbDataFormat(context);
		} catch (JAXBException e) {
			throw new IllegalStateException("Cannot initialize JAXB context: " + e.getMessage(), e);
		}
	}

	public static JacksonDataFormat jackson() {
		return new JacksonDataFormat(AgentList.class);
	}
}
