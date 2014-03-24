package org.opennms.netmgt.sampler.config;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.aries.blueprint.ext.PropertyPlaceholder;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.KeyValueHolder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.AgentList;
import org.opennms.netmgt.api.sample.PackageAgentList;
import org.opennms.netmgt.api.sample.support.SchedulerService;
import org.opennms.netmgt.api.sample.support.SingletonBeanFactory;
import org.opennms.netmgt.config.collectd.CollectdConfiguration;
import org.opennms.netmgt.config.collectd.Package;
import org.opennms.netmgt.config.collectd.Service;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

public class ConfigRouteTest extends CamelBlueprintTestSupport {

    private static final String OPENNMS_HOME = "src/test/resources";

    private static URL url(String path) throws MalformedURLException {
        return new URL("file:" + OPENNMS_HOME + "/" + path);
    }

    private CountDownLatch m_schedulerServiceCalls = null;

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
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void doPostSetup() throws Exception {
        final PropertyPlaceholder properties = bean("properties", PropertyPlaceholder.class);
        final Map defaultProperties = properties.getDefaultProperties();
        defaultProperties.put("opennms.home", "target/test-classes");
        defaultProperties.put("collectdConfigUrl", "file:target/test-classes/etc/collectd-configuration.xml");
        defaultProperties.put("agentListUrl", "file:target/test-classes/agents");
        properties.setDefaultProperties(defaultProperties);
    }

    @BeforeClass
    public static void configureLogging() throws SecurityException, IOException {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.getLogger("org.apache.aries.blueprint").setLevel(Level.INFO);
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

    /**
     * Override 'opennms.home' with the test resource directory.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected String useOverridePropertiesWithConfigAdmin(Dictionary props) throws Exception {
        props.put("opennms.home", OPENNMS_HOME);
        return "org.opennms.netmgt.sampler.config";
    }

    @Test
    public void testParseCollectdXml() throws Exception {
        context.start();

        CollectdConfiguration resultsUsingURL = template.requestBody("direct:parseJaxbXml", new URL("file:" + OPENNMS_HOME + "/etc/collectd-configuration.xml"), CollectdConfiguration.class);

        System.err.printf("Results: %s\n", resultsUsingURL);
        assertNotNull(resultsUsingURL);
        assertEquals(5, resultsUsingURL.getPackages().size());

        CollectdConfiguration resultsUsingString = template.requestBody("direct:parseJaxbXml", "file:" + OPENNMS_HOME + "/etc/collectd-configuration.xml", CollectdConfiguration.class);

        System.err.printf("Results: %s\n", resultsUsingString);
        assertNotNull(resultsUsingString);
        assertNotNull(resultsUsingString);
        assertEquals(5, resultsUsingString.getPackages().size());
    }

    @Test
    public void testParseAgentXml() throws Exception {
        context.start();

        List<Agent> resultsUsingURL = template.requestBody("direct:parseJaxbXml", url("agents/example1/SNMP.xml"), AgentList.class);

        //System.err.printf("Results: %s\n", resultsUsingURL);
        assertNotNull(resultsUsingURL);
        assertEquals(3, resultsUsingURL.size());

        List<Agent> resultsUsingString = template.requestBody("direct:parseJaxbXml", url("agents/example1/SNMP.xml").toString(), AgentList.class);

        //System.err.printf("Results: %s\n", resultsUsingString);
        assertNotNull(resultsUsingString);
        assertEquals(3, resultsUsingString.size());
    }

    /**
     * Test loading the {@link CollectdConfiguration}.
     */
    @Test
    public void testLoadCollectdConfiguration() throws Exception {
        context.start();

        template.requestBody("direct:loadCollectdConfiguration", null, String.class);

        @SuppressWarnings("unchecked")
        SingletonBeanFactory<CollectdConfiguration> configSvc = bean("collectdConfiguration", SingletonBeanFactory.class);

        assertNotNull(configSvc);
        assertNotNull(configSvc.getInstance());
    }

    /**
     * Test loading the {@link PackageAgentList} based on a given collection package.
     */
    @Test
    public void testLoadServiceAgents() throws Exception {
        // Add mock endpoints to the route context
        for (RouteDefinition route : new ArrayList<RouteDefinition>(context.getRouteDefinitions())) {
            route.adviceWith(context, new AdviceWithRouteBuilder() {
                @Override
                public void configure() throws Exception {
                    mockEndpoints();
                }
            });
        }
        context.start();

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
        // Add mock endpoints to the route context
        for (RouteDefinition route : new ArrayList<RouteDefinition>(context.getRouteDefinitions())) {
            route.adviceWith(context, new AdviceWithRouteBuilder() {
                @Override
                public void configure() throws Exception {
                    mockEndpoints();
                }
            });
        }
        context.start();

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
        // Add mock endpoints to the route context
        for (RouteDefinition route : new ArrayList<RouteDefinition>(context.getRouteDefinitions())) {
            route.adviceWith(context, new AdviceWithRouteBuilder() {
                @Override
                public void configure() throws Exception {
                    mockEndpoints();
                }
            });
        }
        context.start();
        // Wait for one call to the scheduler service in a mock OSGi service
        m_schedulerServiceCalls = new CountDownLatch(1);

        MockEndpoint result = getMockEndpoint("mock:direct:schedulerStart", false);
        result.expectedMessageCount(1);

        MockEndpoint scheduled = getMockEndpoint("mock:seda:scheduleAgents", false);
        scheduled.expectedMessageCount(1);

        template.sendBody("direct:start", null);
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
