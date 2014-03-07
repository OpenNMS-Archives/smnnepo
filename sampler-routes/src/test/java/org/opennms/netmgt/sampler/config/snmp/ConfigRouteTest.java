package org.opennms.netmgt.sampler.config.snmp;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.junit.Test;
import org.opennms.netmgt.api.sample.PackageAgentList;
import org.opennms.netmgt.api.sample.ServiceAgent;
import org.opennms.netmgt.api.sample.ServiceAgent.ServiceAgentList;
import org.opennms.netmgt.api.sample.support.SingletonBeanFactory;
import org.opennms.netmgt.config.collectd.CollectdConfiguration;
import org.opennms.netmgt.config.collectd.Package;
import org.opennms.netmgt.config.collectd.Service;
import org.opennms.netmgt.snmp.SnmpConfiguration;

public class ConfigRouteTest extends CamelBlueprintTestSupport {
	
	private static final String OPENNMS_HOME = "src/test/resources";

	private static URL url(String path) throws MalformedURLException {
		return new URL("file:" + OPENNMS_HOME + "/" + path);
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
		return "file:routes-standalone.xml";
	}

	/**
	 * Override 'opennms.home' with the test resource directory.
	 */
	@Override
	protected String useOverridePropertiesWithConfigAdmin(Dictionary props) throws Exception {
		props.put("opennms.home", OPENNMS_HOME);
		return "org.opennms.netmgt.sampler.snmp";
	}

	@Test
	public void testParseXML() throws Exception {
		context.start();

		SnmpConfiguration resultsUsingURL = template.requestBody("direct:parseXML", new URL("file:" + OPENNMS_HOME + "/etc/snmp-config.xml"), SnmpConfiguration.class);

		//System.err.printf("Results: %s\n", resultsUsingURL);
		assertNotNull(resultsUsingURL);
		
		SnmpConfiguration resultsUsingString = template.requestBody("direct:parseXML", "file:" + OPENNMS_HOME + "/etc/snmp-config.xml", SnmpConfiguration.class);

		//System.err.printf("Results: %s\n", resultsUsingString);
		assertNotNull(resultsUsingString);
	}

	/**
	 * Test the Camel JSON parsing.
	 */
	@Test
	public void testParseJSON() throws Exception {
		context.start();

		List<ServiceAgent> resultsUsingURL = template.requestBody("direct:parseJSON", url("agents/example1/SNMP.json"), ServiceAgentList.class);

		//System.err.printf("Results: %s\n", resultsUsingURL);
		assertNotNull(resultsUsingURL);
		assertEquals(3, resultsUsingURL.size());
		
		List<ServiceAgent> resultsUsingString = template.requestBody("direct:parseJSON", url("agents/example1/SNMP.json").toString(), ServiceAgentList.class);

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
		
		SingletonBeanFactory<SnmpConfiguration> configSvc = bean("snmpConfiguration", SingletonBeanFactory.class);
		
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
		
		MockEndpoint result = getMockEndpoint("mock:direct:schedulerStart", false);
		result.expectedMessageCount(1);
		
		MockEndpoint scheduled = getMockEndpoint("mock:seda:scheduleAgents", false);
		scheduled.expectedMessageCount(2);
		
		template.sendBody("seda:start", null);
		result.await();
		
		SingletonBeanFactory<CollectdConfiguration> collectdConfig = bean("collectdConfiguration", SingletonBeanFactory.class);
		SingletonBeanFactory<SnmpConfiguration> snmpConfig = bean("snmpConfiguration", SingletonBeanFactory.class);		
		
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
