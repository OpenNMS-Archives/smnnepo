package org.opennms.netmgt.api.sample;

import org.junit.Test;
import org.opennms.core.test.xml.JaxbTestUtils;

import javax.xml.bind.JAXBException;

public class JaxbContextTest {

    @Test
    public void testJaxbContext() throws JAXBException {
        JaxbTestUtils.verifyJaxbContext(getClass().getPackage().getName());
    }

    @Test
    public void testManually() throws JAXBException {
        JaxbTestUtils.verifyJaxbContext(JvmCollector.JvmMetric.class);
    }
}
