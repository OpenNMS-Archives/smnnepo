package org.opennms.netmgt.sampler.snmp;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.http.annotations.JUnitHttpServer;
import org.opennms.netmgt.api.sample.AgentList;
import org.opennms.netmgt.api.sample.PackageAgentList;
import org.opennms.netmgt.api.sample.support.SingletonBeanFactory;
import org.opennms.netmgt.api.sample.support.SingletonBeanFactoryImpl;
import org.opennms.netmgt.api.sample.support.UrlNormalizer;
import org.opennms.netmgt.config.collectd.CollectdConfiguration;
import org.opennms.netmgt.config.collectd.Package;
import org.opennms.netmgt.config.collectd.Service;
import org.opennms.netmgt.config.snmp.SnmpConfig;
import org.opennms.netmgt.sampler.config.internal.PackageAgentAggregator;
import org.opennms.netmgt.sampler.config.internal.PackageServiceSplitter;
import org.opennms.netmgt.sampler.config.snmp.SnmpMetricRepository;
import org.springframework.test.context.ContextConfiguration;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:/META-INF/opennms/emptyContext.xml"})
@JUnitHttpServer(port=9162)
public class SamplerRoutingTest extends CamelTestSupport {
    private static final String REST_ROOT = "http://localhost:9162";

    private static URL url(String path) throws MalformedURLException {
        return new URL(REST_ROOT + "/" + path);
    }

    public static class DataFormatUtils {
        public static JaxbDataFormat jaxbXml() {
            try {
                final JAXBContext context = JAXBContext.newInstance(CollectdConfiguration.class, SnmpConfig.class, AgentList.class);
                return new JaxbDataFormat(context);
            } catch (final JAXBException e) {
                throw new IllegalStateException("Cannot initialize JAXB context: " + e.getMessage(), e);
            }
        }
    }

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry registry = super.createRegistry();

        SnmpMetricRepository snmpMetricRepository = new SnmpMetricRepository(
            url("datacollection-config.xml"), 
            url("datacollection/mib2.xml"), 
            url("datacollection/netsnmp.xml"),
            url("datacollection/dell.xml")
        );

        registry.bind("collectdConfiguration", new SingletonBeanFactoryImpl<CollectdConfiguration>());
        registry.bind("snmpConfig", new SingletonBeanFactoryImpl<SnmpConfig>());
        registry.bind("snmpMetricRepository", snmpMetricRepository);
        registry.bind("urlNormalizer", new UrlNormalizer());
        registry.bind("packageServiceSplitter", new PackageServiceSplitter());
        registry.bind("jaxbXml", DataFormatUtils.jaxbXml());

        return registry;
    }

    /**
     * Delay calling context.start() so that you can attach an {@link AdviceWithRouteBuilder}
     * to the context before it starts.
     */
    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    /**
     * Build the route for all of the config parsing messages.
     */
    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {

            @Override
            public void configure() throws Exception {

                // Add exception handlers
                onException(IOException.class)
                .handled(true)
                //.transform().constant(null)
                .logStackTrace(true)
                .stop()
                ;

                // Call this to retrieve a URL in string form or URL form into the JAXB objects they represent
                from("direct:parseJaxbXml")
                .beanRef("urlNormalizer")
                .unmarshal("jaxbXml")
                ;

                // Direct route to fetch the config
                from("direct:collectdConfig")
                .beanRef("collectdConfiguration", "getInstance")
                ;

                // Direct route to fetch the config
                from("direct:snmpConfig")
                .beanRef("snmpConfig", "getInstance")
                ;

                from("seda:start")
                // Load all of the configs
                .multicast()
                .parallelProcessing().to(
                    "direct:loadCollectdConfiguration",
                    "direct:loadDataCollectionConfig",
                    "direct:loadSnmpConfig"
                )
                .end()
                .log("==== Configurations Loaded ====")
                // Launch the scheduler
                .to("direct:schedulerStart")
                ;

                // TODO: Create a reload timer that will check for changes to the config
                from("direct:loadCollectdConfiguration")
                .transform(constant(url("collectd-configuration.xml")))
                .to("direct:parseJaxbXml")
                .beanRef("collectdConfiguration", "setInstance")
                ;

                // TODO: Create a reload timer that will check for changes to the config
                from("direct:loadDataCollectionConfig")
                .log("Refreshing snmpMetricRepository")
                .beanRef("snmpMetricRepository", "refresh")
                ;

                // TODO: Create a reload timer that will check for changes to the config
                from("direct:loadSnmpConfig")
                .transform(constant(url("snmp-config.xml")))
                .to("direct:parseJaxbXml")
                .beanRef("snmpConfig", "setInstance")
                ;

                from("direct:schedulerStart")
                .to("direct:loadCollectionPackages")
                ;

                // Get all of the collection packages that are associated with the current package
                from("direct:loadCollectionPackages")
                // Replace the current message with the CollectdConfiguration
                .enrich("direct:collectdConfig")
                // Split the CollectdConfiguration into a list of the packages that it contains
                .log("Parsing CollectdConfiguration with ${body.packages.size} package(s)")
                .transform().simple("${body.packages}")
                .split().body()
                // Split the package into a package-per-service
                .log("Parsing package ${body.name} with ${body.services.size} service(s)")
                .process(new PackageServiceSplitter())
                .split().body()
                // Route different service types to different routes
                .choice()
                .when(simple("${body.services[0].name} == 'SNMP'"))
                .to("seda:loadSnmpAgents")
                .when(property("${body.services[0].name == 'JMX'}"))
                .to("seda:loadJmxAgents")
                /*
		.otherwise()
		.throwException(new UnsupportedOperationException("Cannot process service ${body}"))
                 */
                ;

                from("seda:loadSnmpAgents")
                .log("Running seda:loadSnmpAgents")
                .to("seda:loadPackageAgents")
                ;

                from("seda:loadJmxAgents")
                .log("Running seda:loadJmxAgents")
                .to("seda:loadPackageAgents")
                ;

                from("seda:loadPackageAgents")
                .log("Package before: ${body}")
                .enrich("direct:getServiceAgents", new PackageAgentAggregator())
                .log("Package after: ${body.package}, Agents: ${body.agents}")
                .to("seda:scheduleAgents")
                ;

                from("direct:getServiceAgents")
                .log("Parsing URL: " + REST_ROOT + "/agents/${body.name}/${body.services[0].name}.xml")
                .transform().simple(REST_ROOT + "/agents/${body.name}/${body.services[0].name}.xml")
                .to("direct:parseJaxbXml")
                ;

                from("seda:scheduleAgents")
                .log("TODO: IMPLEMENT seda:scheduleAgents")
                ;
            }
        };
    }

    @Test
    public void testParseSnmpXml() throws Exception {
        context.start();

        SnmpConfig resultsUsingURL = template.requestBody("direct:parseJaxbXml", url("snmp-config.xml"), SnmpConfig.class);

        //System.err.printf("Results: %s\n", resultsUsingURL);
        assertNotNull(resultsUsingURL);

        SnmpConfig resultsUsingString = template.requestBody("direct:parseJaxbXml", url("snmp-config.xml").toString(), SnmpConfig.class);

        //System.err.printf("Results: %s\n", resultsUsingString);
        assertNotNull(resultsUsingString);
    }

    @Test
    public void testParseAgentXml() throws Exception {
        context.start();

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
        context.start();

        template.requestBody("direct:loadCollectdConfiguration", null, String.class);

        SingletonBeanFactory<CollectdConfiguration> configSvc = bean("collectdConfiguration", SingletonBeanFactory.class);

        assertNotNull(configSvc);
        assertNotNull(configSvc.getInstance());
    }

    @Test
    public void testLoadSnmpConfig() throws Exception {
        context.start();

        template.requestBody("direct:loadSnmpConfig", null, String.class);

        SingletonBeanFactory<SnmpConfig> configSvc = bean("snmpConfig", SingletonBeanFactory.class);

        assertNotNull(configSvc);
        assertNotNull(configSvc.getInstance());

    }

    /**
     * Test loading the {@link SnmpMetricRepository}.
     */
    @Test
    public void testLoadDataCollectionConfig() throws Exception {
        context.start();

        template.requestBody("direct:loadDataCollectionConfig", null, String.class);

        SnmpMetricRepository metricRepo = bean("snmpMetricRepository", SnmpMetricRepository.class);

        assertNotNull(metricRepo);
        assertNotNull(metricRepo.getMetric("ifInOctets"));

    }

    /**
     * Test loading the {@link PackageAgentList} based on a given collection package.
     */
    @Test
    public void testLoadServiceAgents() throws Exception {
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

        template.requestBody("seda:loadSnmpAgents", pkg);

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
        for (RouteDefinition route : new ArrayList<RouteDefinition>(context.getRouteDefinitions())) {
            route.adviceWith(context, new AdviceWithRouteBuilder() {
                @Override
                public void configure() throws Exception {
                    mockEndpoints();
                }
            });
        }
        context.start();

        MockEndpoint result = getMockEndpoint("mock:direct:schedulerStart", false);
        result.expectedMessageCount(1);

        MockEndpoint scheduled = getMockEndpoint("mock:seda:scheduleAgents", false);
        scheduled.expectedMessageCount(2);

        template.sendBody("seda:start", null);
        result.await();

        SingletonBeanFactory<CollectdConfiguration> collectdConfig = bean("collectdConfiguration", SingletonBeanFactory.class);
        SingletonBeanFactory<SnmpConfig> snmpConfig = bean("snmpConfig", SingletonBeanFactory.class);		

        assertMockEndpointsSatisfied();

        assertNotNull(collectdConfig);
        assertNotNull(collectdConfig.getInstance());

        assertNotNull(snmpConfig);
        assertNotNull(snmpConfig.getInstance());
    }

    private <T> T bean(String name,	Class<T> type) {
        return context().getRegistry().lookupByNameAndType(name, type);
    }
}
