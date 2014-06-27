package org.opennms.minion.impl;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.camel.converter.jaxb.JaxbDataFormat;

public abstract class DataFormatUtils {
    public static JaxbDataFormat getDataFormat() {
        try {
            final JAXBContext context = JAXBContext.newInstance(MinionStatusMessageImpl.class);
            return new JaxbDataFormat(context);
        } catch (final JAXBException e) {
            throw new IllegalStateException("Cannot initialize JAXB context: " + e.getMessage(), e);
        }
    }
}
