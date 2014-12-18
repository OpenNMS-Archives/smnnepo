package org.opennms.minion.test;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.xml.JaxbTestUtils;
import org.opennms.minion.test.core.SmnnepoKarafTest;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.AgentList;
import org.opennms.netmgt.api.sample.DataFormatUtils;
import org.opennms.netmgt.api.sample.Sample;
import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.config.collectd.CollectdConfiguration;
import org.opennms.netmgt.config.collectd.jmx.JmxDatacollectionConfig;
import org.opennms.netmgt.config.snmp.SnmpConfig;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import static org.ops4j.pax.exam.CoreOptions.maven;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class JaxbContextKarafTest extends SmnnepoKarafTest {

    @Before
    public void registerTestFeatures() {
        // TODO remove fix version and use versionAsInProject instead (see PJSM-244)
        addFeaturesUrl(maven().groupId("org.opennms.netmgt.sample").artifactId("integration-tests").version("15.0.0-PJSM-SNAPSHOT").type("xml").getURL());
    }

    @Test
    public void testJaxbContextSingleClass() throws JAXBException {
        installFeature("opennms-test-api-xml"); // we need this for JaxbTestUtils
        installFeature("sample-api"); // otherwise classes are not known/found

        // Usually we would invoke verifyJaxbContext(Package) and dynamically read all classes in that package
        // Unfortunately this test runs in an osgi container and dynamically read all classes using java reflections
        // does not work. We manually test a few, but not all.
        JaxbTestUtils.verifyJaxbContext(Agent.class);
        JaxbTestUtils.verifyJaxbContext(AgentList.class);
        JaxbTestUtils.verifyJaxbContext(CollectdConfiguration.class);
        JaxbTestUtils.verifyJaxbContext(SnmpConfig.class);
        JaxbTestUtils.verifyJaxbContext(SampleSet.class);
        JaxbTestUtils.verifyJaxbContext(JmxDatacollectionConfig.class);
    }

    /*
     * We encountered issues when creating a JAXBContext while running in karaf, so this tests
     * verifies that the JAXBContext can be created correctly.
     */
    @Test
    public void testCreateJaxbContextMultipleClasses() throws JAXBException {
        installFeature("sample-api");

        JAXBContext jaxbContext = JAXBContext.newInstance(AgentList.class, Agent.class, SnmpConfig.class, SampleSet.class);
        Assert.assertNotNull(jaxbContext);

        jaxbContext = JAXBContext.newInstance(CollectdConfiguration.class,JmxDatacollectionConfig.class);
        Assert.assertNotNull(jaxbContext);

        jaxbContext = JAXBContext.newInstance(Agent.class, Sample.class, CollectdConfiguration.class);
        Assert.assertNotNull(jaxbContext);

        /*
         * The order is important.
         * It first loads bundle sample-api (AgentList.class).
         * The bundle sample-api has a dependency on opennms-config-jaxb, and therefore this works.
         * If the first parameter would be "JmxDataCollectionConfig.class" this no longer works.
         */
        jaxbContext = JAXBContext.newInstance(AgentList.class, Agent.class, SnmpConfig.class, Sample.class, SampleSet.class, JmxDatacollectionConfig.class, CollectdConfiguration.class);
        Assert.assertNotNull(jaxbContext);

    }
}
