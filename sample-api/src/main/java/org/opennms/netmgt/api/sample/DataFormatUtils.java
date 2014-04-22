package org.opennms.netmgt.api.sample;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.opennms.netmgt.config.collectd.CollectdConfiguration;
import org.opennms.netmgt.config.snmp.SnmpConfig;

public abstract class DataFormatUtils {
    public static JaxbDataFormat jaxbXml() {
        try {
            final JAXBContext context = JAXBContext.newInstance(CollectdConfiguration.class, AgentList.class, Agent.class, SnmpConfig.class, SampleSet.class);
            return new JaxbDataFormat(context);
        } catch (final JAXBException e) {
            throw new IllegalStateException("Cannot initialize JAXB context: " + e.getMessage(), e);
        }
    }
}