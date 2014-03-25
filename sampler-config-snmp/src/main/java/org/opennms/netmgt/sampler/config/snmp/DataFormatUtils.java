package org.opennms.netmgt.sampler.config.snmp;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.opennms.netmgt.config.snmp.SnmpConfig;

public abstract class DataFormatUtils {
    public static JaxbDataFormat snmpConfigXml() {
        try {
            JAXBContext context = JAXBContext.newInstance(SnmpConfig.class);
            return new JaxbDataFormat(context);
        } catch (JAXBException e) {
            throw new IllegalStateException("Cannot initialize JAXB context: " + e.getMessage(), e);
        }
    }
}
