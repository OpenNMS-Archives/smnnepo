package org.opennms.minion.test;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.minion.test.core.SmnnepoKarafTest;
import org.opennms.netmgt.api.sample.DataFormatUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.util.List;

/**
 * Tests that issue PJSM-253 is fixed.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class PJSM253KarafTest extends SmnnepoKarafTest {

    private static final Logger LOG = LoggerFactory.getLogger(PJSM253KarafTest.class);

    private static final String JAXB_CONTEXT_RESOURCE = "JAXBContext.class";

    private static final String MOXY_JAXB_CONTEXT_PATH = "org/eclipse/persistence/jaxb";

    @Test
    public void testPJSM253() throws JAXBException {
        /*
         * To reproduce PJSM-253 we have to install jaxb and opennms-core manually
         * and afterwards install sample-api, even if sample-api installs both features anyways.
         * NOTE: Do not change the order.
         */
        verifyNoJaxbContext();
        installFeature("jaxb");
        installFeature("opennms-core");
        installFeature("sample-api");
        verifyJaxbContext();

        org.apache.camel.converter.jaxb.JaxbDataFormat jaxbDataFormat = DataFormatUtils.jaxbXml();
        Assert.assertNotNull(jaxbDataFormat);
    }

    /**
     * Tests that feature "wrapper-activemq-client" does not pull in a jaxb-implementation and therefore
     * causes JAXBContext.newInstance(...) to fail.
     */
    @Test
    public void testPJSM253_2() {
        verifyNoJaxbContext();

        installFeature("wrapper-activemq-client");
        verifyJaxbContext();

        installFeature("sample-api");
        verifyJaxbContext();

        org.apache.camel.converter.jaxb.JaxbDataFormat jaxbDataFormat = DataFormatUtils.jaxbXml();
        Assert.assertNotNull(jaxbDataFormat);
    }

    /**
     * Tests that feature "wrapper-activemq" does not pull in a jaxb-implementation and therefore
     * causes JAXBContext.newInstance(...) to fail.
     */
    @Test
    public void testPJSM253_3() {
        verifyNoJaxbContext();
        installFeature("wrapper-activemq");
        installFeature("sample-api");
        verifyJaxbContext();

        org.apache.camel.converter.jaxb.JaxbDataFormat jaxbDataFormat = DataFormatUtils.jaxbXml();
        Assert.assertNotNull(jaxbDataFormat);
    }

    /**
     * Tests that feature "opennms-activemq-dispatcher" does not pull in a jaxb-implementation and therefore
     * causes JAXBContext.newInstance(...) to fail.
     */
    @Test
    public void testPJSM253_4() {
        verifyNoJaxbContext();
        installFeature("opennms-activemq-dispatcher");
        installFeature("sample-api");
        verifyJaxbContext();

        org.apache.camel.converter.jaxb.JaxbDataFormat jaxbDataFormat = DataFormatUtils.jaxbXml();
        Assert.assertNotNull(jaxbDataFormat);
    }

    private void verifyJaxbContext() {
        List<ResultInfo> resultInfoList = findResource(JAXB_CONTEXT_RESOURCE);
        Assert.assertFalse("There should be at least one " + JAXB_CONTEXT_RESOURCE + " registered.", resultInfoList.isEmpty());
        for (ResultInfo eachInfo : resultInfoList) {
            for (String eachResource : eachInfo.getResources()) {
                LOG.info("Verifiing {}", eachResource);
                Assert.assertTrue("The given resource '" + eachResource + "' is no eclipse moxy resource.", eachResource.startsWith(MOXY_JAXB_CONTEXT_PATH));
            }
        }
    }

    private void verifyNoJaxbContext() {
        List<ResultInfo> resultInfoList = findResource("JAXBContext");
        Assert.assertEquals(0, resultInfoList.size());
    }
}
