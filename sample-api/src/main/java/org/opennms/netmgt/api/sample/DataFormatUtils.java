package org.opennms.netmgt.api.sample;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.opennms.netmgt.config.collectd.CollectdConfiguration;
import org.opennms.netmgt.config.collectd.jmx.JmxDatacollectionConfig;
import org.opennms.netmgt.config.snmp.SnmpConfig;

public abstract class DataFormatUtils {
    public static JaxbDataFormat jaxbXml() {
        try {
             /*
             * The order is important.
             * It first loads bundle sample-api (AgentList.class).
             * The bundle sample-api has a dependency on opennms-config-jaxb, and therefore this works.
             * If the first parameter would be "JmxDataCollectionConfig.class" this no longer works.
             */
            final JAXBContext context = JAXBContext.newInstance(AgentList.class, Agent.class, SnmpConfig.class, SampleSet.class, JmxDatacollectionConfig.class, CollectdConfiguration.class);
            return new JaxbDataFormat(context);
        } catch (final JAXBException e) {
            throw new IllegalStateException("Cannot initialize JAXB context: " + e.getMessage(), e);
        }
    }
}