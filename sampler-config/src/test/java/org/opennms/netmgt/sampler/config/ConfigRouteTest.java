package org.opennms.netmgt.sampler.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.util.KeyValueHolder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.camel.CamelBlueprintTest;
import org.opennms.core.test.http.annotations.JUnitHttpServer;
import org.opennms.netmgt.api.sample.AgentList;
import org.opennms.netmgt.api.sample.PackageAgentList;
import org.opennms.netmgt.api.sample.support.SchedulerService;
import org.opennms.netmgt.api.sample.support.SingletonBeanFactory;
import org.opennms.netmgt.config.collectd.CollectdConfiguration;
import org.opennms.netmgt.config.collectd.Package;
import org.opennms.netmgt.config.collectd.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:/META-INF/opennms/emptyContext.xml"})
@JUnitHttpServer(port=9162)
public class ConfigRouteTest extends CamelBlueprintTest {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigRouteTest.class);
    private static final String OPENNMS_HOME = "target/test-classes";
    private static final String REST_ROOT = "http://localhost:9162";

    private static URL url(final String path) throws MalformedURLException {
        return new URL(REST_ROOT + "/" + path);
    }

    private CountDownLatch m_schedulerServiceCalls = null;

    /**
     * Register a mock OSGi {@link SchedulerService} so that we can make sure that
     * the scheduler whiteboard is working properly.
     */
    @SuppressWarnings("rawtypes")
    @Override
    protected void addServicesOnStartup(Map<String, KeyValueHolder<Object, Dictionary>> services) {
        services.put(SchedulerService.class.getName(), new KeyValueHolder<Object,Dictionary>(new SchedulerService() {
            @Override
            public void schedule(PackageAgentList agents) {
                System.err.println("Calling SchedulerService.schedule()");
                if (m_schedulerServiceCalls != null) {
                    m_schedulerServiceCalls.countDown();
                }
            }
        }, new Properties()));
    }

    // The location of our Blueprint XML file to be used for testing
    @Override
    protected String getBlueprintDescriptor() {
        return "file:src/main/resources/OSGI-INF/blueprint/blueprint-sampler-config.xml";
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        Properties props = new Properties();
        props.put("opennms.home", OPENNMS_HOME);
        props.put("collectdConfigUrl", REST_ROOT + "/etc/collectd-configuration.xml");
        props.put("agentListUrl", REST_ROOT + "/agents");
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
        props.put("opennms.home", OPENNMS_HOME);
        props.put("collectdConfigUrl", REST_ROOT + "/etc/collectd-configuration.xml");
        props.put("agentListUrl", REST_ROOT + "/agents");
        return "org.opennms.netmgt.sampler.config";
    }

    @Test
    public void testParseCollectdXml() throws Exception {

        CollectdConfiguration resultsUsingURL = template.requestBody("direct:parseJaxbXml", new URL(REST_ROOT + "/etc/collectd-configuration.xml"), CollectdConfiguration.class);

        System.err.printf("Results: %s\n", resultsUsingURL);
        assertNotNull(resultsUsingURL);
        assertEquals(5, resultsUsingURL.getPackages().size());

        CollectdConfiguration resultsUsingString = template.requestBody("direct:parseJaxbXml", REST_ROOT + "/etc/collectd-configuration.xml", CollectdConfiguration.class);

        System.err.printf("Results: %s\n", resultsUsingString);
        assertNotNull(resultsUsingString);
        assertNotNull(resultsUsingString);
        assertEquals(5, resultsUsingString.getPackages().size());
        
        // now try a config from Ben's OpenNMS instance
        resultsUsingURL = template.requestBody("direct:parseJaxbXml", new URL(REST_ROOT + "/etc/collectd-configuration-rr.xml"), CollectdConfiguration.class);
        assertNotNull(resultsUsingURL);
        assertEquals(1, resultsUsingURL.getPackages().size());
        assertEquals(4, resultsUsingURL.getPackages().get(0).getServices().size());
    }

    @Test
    public void testParseAgentXml() throws Exception {

        AgentList resultsUsingURL = template.requestBody("direct:parseJaxbXml", url("agents/example1/SNMP.xml"), AgentList.class);

        //System.err.printf("Results: %s\n", resultsUsingURL);
        assertNotNull(resultsUsingURL);
        assertEquals(3, resultsUsingURL.size());

        AgentList resultsUsingString = template.requestBody("direct:parseJaxbXml", url("agents/example1/SNMP.xml").toString(), AgentList.class);

        //System.err.printf("Results: %s\n", resultsUsingString);
        assertNotNull(resultsUsingString);
        assertEquals(3, resultsUsingString.size());
    }

    /**
     * Test loading the {@link CollectdConfiguration}.
     */
    @Test
    public void testLoadCollectdConfiguration() throws Exception {

        template.requestBody("direct:loadCollectdConfiguration", null, String.class);

        @SuppressWarnings("unchecked")
        SingletonBeanFactory<CollectdConfiguration> configSvc = bean("collectdConfiguration", SingletonBeanFactory.class);

        assertNotNull(configSvc);
        LOG.debug("configSvc = {}", configSvc);
        assertNotNull(configSvc.getInstance());
        System.err.println(configSvc.getInstance());
    }

    /**
     * Test loading the {@link PackageAgentList} based on a given collection package.
     */
    @Test
    public void testLoadServiceAgents() throws Exception {

        // We should get 1 call to the scheduler endpoint
        assertTrue(context.hasEndpoint("mock:seda:scheduleAgents") != null);
        MockEndpoint endpoint = getMockEndpoint("mock:seda:scheduleAgents", false);
        endpoint.setExpectedMessageCount(1);

        Service svc = new Service();
        svc.setInterval(1000L);
        svc.setName("SNMP");
        svc.setStatus("on");
        svc.setUserDefined("false");

        Package pkg = new Package();
        pkg.setName("example1");
        pkg.setServices(Collections.singletonList(svc));

        template.requestBody("seda:loadPackageAgents", pkg);

        assertMockEndpointsSatisfied();

        // Make sure that we got one exchange to the scheduler
        assertEquals(1, endpoint.getReceivedCounter());
        // That contains 3 SNMP agent instances
        for (Exchange exchange : endpoint.getReceivedExchanges()) {
            PackageAgentList agents = exchange.getIn().getBody(PackageAgentList.class);
            assertNotNull(agents);
            assertEquals(3, agents.getAgents().size());
        }
    }

    @Test
    public void testLoadPackageServiceList() throws Exception {

        MockEndpoint result = getMockEndpoint("mock:seda:scheduleAgents", false);
        result.expectedMessageCount(1);

        // Load the CollectdConfiguration
        template.sendBody("direct:loadCollectdConfiguration", null);

        // Fetch the loaded config
        CollectdConfiguration collectConfig = template.requestBody("direct:collectdConfig", null, CollectdConfiguration.class);
        // Pass it to the method that parses the config into a package/service object
        template.sendBody("direct:loadCollectionPackages", collectConfig);

        assertMockEndpointsSatisfied();

        assertEquals(1, result.getReceivedCounter());
        for (Exchange exchange : result.getReceivedExchanges()) {
            assertEquals(3, exchange.getIn().getBody(PackageAgentList.class).getAgents().size());
        }
    }

    @Test(timeout=15000)
    public void testStartup() throws Exception {

        // Wait for one call to the scheduler service in a mock OSGi service
        m_schedulerServiceCalls = new CountDownLatch(1);

        MockEndpoint result = getMockEndpoint("mock:direct:loadCollectionPackages", false);
        result.expectedMessageCount(1);

        MockEndpoint scheduled = getMockEndpoint("mock:seda:scheduleAgents", false);
        scheduled.expectedMessageCount(1);

        template.sendBody("direct:loadConfigurations", null);
        result.await();

        @SuppressWarnings("unchecked")
        SingletonBeanFactory<CollectdConfiguration> collectdConfig = bean("collectdConfiguration", SingletonBeanFactory.class);

        assertMockEndpointsSatisfied();

        assertNotNull(collectdConfig);
        assertNotNull(collectdConfig.getInstance());
        m_schedulerServiceCalls.await();
    }

    private <T> T bean(String name, Class<T> type) {
        return context().getRegistry().lookupByNameAndType(name, type);
    }
}
