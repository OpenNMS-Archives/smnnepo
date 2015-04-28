package org.opennms.netmgt.sampler.jmx;

import java.net.URL;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.KeyValueHolder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.http.annotations.JUnitHttpServer;
import org.opennms.netmgt.api.sample.support.SingletonBeanFactory;
import org.opennms.netmgt.api.sample.support.SingletonBeanFactoryImpl;
import org.opennms.netmgt.config.collectd.CollectdConfiguration;
import org.opennms.netmgt.config.collectd.jmx.JmxCollection;
import org.opennms.netmgt.config.collectd.jmx.JmxDatacollectionConfig;
import org.springframework.test.context.ContextConfiguration;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:/META-INF/opennms/emptyContext.xml"})
@JUnitHttpServer(port=9162)
public class JmxConfigRoutesTest extends CamelBlueprintTestSupport {
    private static final String REST_ROOT = "http://localhost:9162";

    /**
     * Use Aries Blueprint synchronous mode to avoid a blueprint
     * deadlock bug.
     * 
     * @see https://issues.apache.org/jira/browse/ARIES-1051
     * @see https://access.redhat.com/site/solutions/640943
     */
    @Override
    public void doPreSetup() throws Exception {
        System.setProperty("org.apache.aries.blueprint.synchronous", Boolean.TRUE.toString());
        System.setProperty("de.kalpatec.pojosr.framework.events.sync", Boolean.TRUE.toString());
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    public boolean isUseDebugger() {
        // must enable debugger
        return true;
    }

    // The location of our Blueprint XML file to be used for testing
    @Override
    protected String getBlueprintDescriptor() {
        return "file:src/main/resources/OSGI-INF/blueprint/blueprint.xml";
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = new Properties();
        props.put("jmxDatacollectionConfigUrl", REST_ROOT + "/etc/jmx-datacollection-config.xml");
        return props;
    }

    /**
     * We have to use {@link #useOverridePropertiesWithPropertiesComponent()} and
     * {@link #useOverridePropertiesWithConfigAdmin(Dictionary)} because there are
     * beans outside of the Camel context that use CM properties.
     */
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected String useOverridePropertiesWithConfigAdmin(Dictionary props) throws Exception {
        props.put("jmxDatacollectionConfigUrl", REST_ROOT + "/etc/jmx-datacollection-config.xml");
        return "org.opennms.netmgt.sampler.config.snmp.jmx";
    }

    @Test
    public void testParseJmxXml() throws Exception {
        System.err.printf("Starting testParseJmxXml");
        JmxDatacollectionConfig resultsUsingURL = template.requestBody("direct:parseJmxXml", new URL(REST_ROOT + "/etc/jmx-datacollection-config.xml"), JmxDatacollectionConfig.class);

        System.err.printf("Results Using URL: %s\n", resultsUsingURL);
        assertNotNull(resultsUsingURL);

        JmxCollection jmxCollection = resultsUsingURL.getJmxCollection("jsr160");
        assertNotNull(jmxCollection);

        assertEquals(23, jmxCollection.getMbeanCount());
    }

    @Test
    public void testLoadJmxDatacollectionConfig() throws Exception {
        System.err.printf("Starting testLoadJmxDatacollectionConfig");

        template.requestBody("direct:loadJmxDatacollectionConfig", null, String.class);

        @SuppressWarnings("unchecked")
        SingletonBeanFactory<JmxDatacollectionConfig> configSvc = bean("jmxConfigFactory", SingletonBeanFactory.class);

        System.err.printf("configSvc: %s\n", configSvc);
        assertNotNull(configSvc);
        assertNotNull(configSvc.getInstance());
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void addServicesOnStartup(Map<String, KeyValueHolder<Object, Dictionary>> services) {
        CollectdConfiguration collectdConfig = new CollectdConfiguration();

        Properties props = new Properties();
        props.put("beanClass", "org.opennms.netmgt.config.collectd.CollectdConfiguration");
        services.put(SingletonBeanFactory.class.getName(), new KeyValueHolder<Object,Dictionary>(new SingletonBeanFactoryImpl<CollectdConfiguration>(collectdConfig), props));
    }

    private <T> T bean(String name,	Class<T> type) {
        return context().getRegistry().lookupByNameAndType(name, type);
    }
}
