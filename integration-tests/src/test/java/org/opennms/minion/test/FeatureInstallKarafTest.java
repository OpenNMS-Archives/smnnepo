package org.opennms.minion.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.minion.test.core.SmnnepoKarafTest;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

/**
 * This tests checks if features are defined properly and can be installed
 * without any other features/bundles installed by default.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class FeatureInstallKarafTest extends SmnnepoKarafTest {

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
    public void testSampler() {
        installFeature("opennms-sampler");
    }

    @Test
    public void testSamplerScheduler() {
        installFeature("sampler-scheduler");
    }

    @Test
    public void testSamplerSnmp() {
        installFeature("sampler-snmp");
    }

    @Test
    public void testSamplerJmx() {
        installFeature("sampler-jmx");
    }

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

    @Test
    public void testSampleStorageRrd() {
        installFeature("sample-storage-rrd");
    }
}
