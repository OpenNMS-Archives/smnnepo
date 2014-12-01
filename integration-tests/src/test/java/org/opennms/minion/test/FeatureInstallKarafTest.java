package org.opennms.minion.test;

import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionBaseConfigurationOption;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;

import static org.ops4j.pax.exam.CoreOptions.maven;

/**
 * This tests checks if features are defined properly and can be installed
 * without any other features/bundles installed by default.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
// TODO mvr run as IT (skip by default?)
// TODO mvr fix logging (is currently not working)
// TODO mvr make installFeature verbose
public class FeatureInstallKarafTest extends KarafTestCase {

    @Before
    public void registerOpenNMSFeatuers() {
        // TODO remove fix version and use versionAsInProject instead (see PJSM-244)
        addFeaturesUrl(maven().groupId("org.opennms.netmgt.sample").artifactId("karaf").version("15.0.0-PJSM-SNAPSHOT").type("xml").getURL());
    }

    @Test
    public void testActiveMq() {
        installFeature("activemq");
    }

    @Test
    public void testCamelBlueprint() {
        installFeature("camel-blueprint");
    }

    @Test
    public void testOpennmsCore() {
        installFeature("opennms-core");
    }

    @Test
    public void testOpennmsConfigJaxb() {
        installFeature("opennms-config-jaxb");
    }

    @Test
    public void testOpennmsSnmp() {
        installFeature("opennms-snmp");
    }

    @Test
    public void testSampleApi() {
        installFeature("sample-api");
    }

    @Test
    public void testSamplerConfig() {
        installFeature("sampler-config");
    }

    @Test
    public void testSamplerScheduler() {
        installFeature("sampler-scheduler");
    }

    @Test
    public void testSamplerSnmp() {
        installFeature("sampler-snmp");
    }

//    // TODO Test sampler jmx
//    @Test
//    public void testSamplerJmx() {
//        installFeature("sampler-jmx");
//    }

    @Test
    public void testSampleDispatchActivemq() {
        installFeature("sample-dispatch-activemq");
    }

    @Test
    public void testSamplerWithActiveMqExport() {
        installFeature("sampler-with-activemq-export");
    }

    @Test
    public void testMinionBase() {
        installFeature("minion-base");
    }

    // TODO mvr set location in org.opennms.minion.controller to prevent MinionException "Location is not Set"
    @Test
    public void testMinionController() {
        installFeature("minion-controller");
    }

    @Test
    public void testOpennmsActivemqDispatcher() {
        installFeature("opennms-activemq-dispatcher");
    }
}
