package org.opennms.netmgt.sampler.jmx;

import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.KeyValueHolder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.netmgt.api.sample.support.SingletonBeanFactory;
import org.opennms.netmgt.api.sample.support.SingletonBeanFactoryImpl;
import org.opennms.netmgt.config.collectd.jmx.JmxDatacollectionConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={
    "classpath:/jmxCollectorTest-context.xml"
})
public class JmxCollectorTest extends CamelBlueprintTestSupport {

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
    public boolean isUseDebugger() {
        // must enable debugger
        return true;
    }

    // The location of our Blueprint XML file to be used for testing
    @Override
    protected String getBlueprintDescriptor() {
        return "file:src/main/resources/OSGI-INF/blueprint/blueprint.xml";
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void addServicesOnStartup(Map<String, KeyValueHolder<Object, Dictionary>> services) {
        JmxDatacollectionConfig jmxConfig = new JmxDatacollectionConfig();

        Properties props = new Properties();
        props.put("beanClass", "org.opennms.netmgt.config.collectd.jmx.JmxDatacollectionConfig");
        services.put(SingletonBeanFactory.class.getName(), new KeyValueHolder<Object,Dictionary>(new SingletonBeanFactoryImpl<JmxDatacollectionConfig>(jmxConfig), props));
    }

    @Test
    public void canLoadBlueprint() throws Exception {
        // Verifies that we can successfully load the blueprint
    }
}
