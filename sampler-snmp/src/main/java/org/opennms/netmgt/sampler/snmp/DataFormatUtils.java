package org.opennms.netmgt.sampler.snmp;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.opennms.netmgt.config.collectd.CollectdConfiguration;
import org.opennms.netmgt.sampler.snmp.ServiceAgent.ServiceAgentList;
import org.opennms.netmgt.snmp.SnmpConfiguration;

public abstract class DataFormatUtils {
	public static JaxbDataFormat jaxb() {
		try {
			JAXBContext context = JAXBContext.newInstance(CollectdConfiguration.class, SnmpConfiguration.class);
			return new JaxbDataFormat(context);
		} catch (JAXBException e) {
			throw new IllegalStateException("Cannot initialize JAXB context: " + e.getMessage(), e);
		}
	}

	public static JacksonDataFormat jackson() {
		return new JacksonDataFormat(ServiceAgentList.class);
	}
}
