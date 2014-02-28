package org.opennms.netmgt.sampler.snmp;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBContext;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.support.SingletonBeanFactory;
import org.opennms.netmgt.sampler.config.snmp.SnmpMetricRepository;
import org.opennms.netmgt.sampler.snmp.ServiceAgent.ServiceAgentList;

public class SamplerRoutingTest extends CamelTestSupport {
	
	public static final int AGENT_COUNT = 1;
	
	private static URL url(String path) throws MalformedURLException {
		return new URL("file:src/test/resources/" + path);
	}

	private static class AgentList extends ArrayList<Agent> {}

	public static class UrlNormalizer {

		public URL toURL(String s) throws MalformedURLException {
			URL url = new URL(s);
			//System.err.printf("Converting String '%s' to url: '%s'\n", s, url);
			return url;
		}

		public URL toURL(URL u) throws MalformedURLException {
			//System.err.printf("No need to convert URL '%s' to a url\n", u);
			return u;
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
		
		registry.bind("collectdConfigurationService", new SingletonBeanFactory<CollectdConfiguration>());
		registry.bind("snmpConfigurationService", new SingletonBeanFactory<SnmpConfiguration>());;
		registry.bind("snmpMetricRepository", snmpMetricRepository);
		registry.bind("urlNormalizer", new UrlNormalizer());
		
		return registry;
	}
	

	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {

			@Override
			public void configure() throws Exception {
				
				JAXBContext context = JAXBContext.newInstance(CollectdConfiguration.class, SnmpConfiguration.class);

				JaxbDataFormat jaxb = new JaxbDataFormat(context);
				JacksonDataFormat json = new JacksonDataFormat(ServiceAgentList.class);
				//JacksonDataFormat json = new JacksonDataFormat();
				
				// Call this to retrieve a url in string form or URL form into the jaxb objects they represent
				from("direct:parseXML")
					.beanRef("urlNormalizer")
					.unmarshal(jaxb)
				;
				
				// Call this to retrieve a url in string form or URL form into the json objects they represent
				from("direct:parseJSON")
					.beanRef("urlNormalizer")
					.unmarshal(json)
				;

				// Direct route to fetch the config
				from("direct:collectdConfig")
					.beanRef("collectdConfigurationService", "getInstance")
				;

				// Direct route to fetch the config
				from("direct:snmpConfig")
					.beanRef("snmpConfigurationService", "getInstance")
				;

				from("seda:start")
					// load all of the configs
					.multicast()
						.parallelProcessing().to("direct:loadCollectdConfiguration","direct:loadDataCollectionConfig","direct:loadSnmpConfig")
					.end()
					.log("==== Configurations Loaded ====")
					.to("direct:schedulerStart")
					.to("mock:result")
				;

				// TODO: Create a reload timer that will check for changes to the config
				from("direct:loadCollectdConfiguration")
					.transform(constant(url("collectd-configuration.xml")))
					.to("direct:parseXML")
					.beanRef("collectdConfigurationService", "setInstance")
				;
				
				// TODO: Create a reload timer that will check for changes to the config
				from("direct:loadDataCollectionConfig")
					.log("Refreshing snmpMetricRepository")
					.beanRef("snmpMetricRepository", "refresh")
				;
				
				// TODO: Create a reload timer that will check for changes to the config
				from("direct:loadSnmpConfig")
					.transform(constant(url("snmp-config.xml")))
					.to("direct:parseXML")
					.beanRef("snmpConfigurationService", "setInstance")
				;
				
				from("direct:schedulerStart")
					.to("direct:loadPackageServiceList")
				;
				
				// Get all of the collection packages that are associated with the current package
				from("direct:loadPackageServiceList")
					// Replace the current message with the CollectdConfiguration
					.enrich("direct:collectdConfig")
					// Process it into packages
					.process(new Processor() {
						@Override
						public void process(Exchange exchange) throws Exception {
							// TODO: Pull the packages out of the CollectdConfiguration
							PackageService[] svcs = new PackageService[] {
									new PackageService("example1", "SNMP", 300000, "example1"),
									new PackageService("example1", "JMX", 300000, "example1")
							};

							exchange.getIn().setBody(Arrays.asList(svcs));
						}
					})
					// For each package...
					.split().body()
						.log("PackageService: ${body}")
						// Load the agents for the package
						.to("seda:loadPackageAgents")
						/*
						.choice()
							.when(body().isEqualTo("SNMP"))
								.aggregate(new ArrayListAggregationStrategy())
								//.setHeader("service", constant("SNMP"))
								.beanRef("collectdConfigurationService", "getPackageServiceList")
								.setHeader("config", from("direct:collectdConfig"))
								.to("seda:loadSnmpAgents")
							.when(body().isEqualTo("JMX"))
								.setHeader("service", constant("JMX"))
								.to("seda:loadJmxAgents")
							.otherwise()
								.throwException(new UnsupportedOperationException("Cannot process service ${body}"))
						.end()
						*/
				;
				
				/*
				from("seda:processPackageService")
					.log("Processing Service: ${body}")
					.to("direct:loadServiceAgents")
					.to("seda:scheduleServiceAgents")
				;
				*/

				from("seda:loadPackageAgents")
					.enrich("direct:getServiceAgents", new AggregationStrategy() {
						
						@Override
						public Exchange aggregate(Exchange pkgServiceExchange, Exchange svcAgentsExchange) {
							PackageService pkgService = pkgServiceExchange.getIn().getBody(PackageService.class);
							List<ServiceAgent> svcAgents = svcAgentsExchange.getIn().getBody(ServiceAgentList.class);
							
							PackageAgentList pkgAgents = new PackageAgentList(pkgService, svcAgents);
							
							pkgServiceExchange.getIn().setBody(pkgAgents);
							
							return pkgServiceExchange;
						}
					})
					//.beanRef("collectdConfigurationService", "setPackageAgentList")
				;

				from("direct:getServiceAgents")
					.transform().simple("file:src/test/resources/agents/${body.filterName}/${body.svcName}.json")
					.to("direct:parseJSON")
				;

			}
			
		};
	}
	
	@Test
	public void testParseXML() throws Exception {

		SnmpConfiguration resultsUsingURL = template.requestBody("direct:parseXML", url("snmp-config.xml"), SnmpConfiguration.class);

		//System.err.printf("Results: %s\n", resultsUsingURL);
		assertNotNull(resultsUsingURL);
		
		SnmpConfiguration resultsUsingString = template.requestBody("direct:parseXML", url("snmp-config.xml").toString(), SnmpConfiguration.class);

		//System.err.printf("Results: %s\n", resultsUsingString);
		assertNotNull(resultsUsingString);
	}

	@Test
	public void testParseJSON() throws Exception {
		List<ServiceAgent> resultsUsingURL = template.requestBody("direct:parseJSON", url("agents/example1/SNMP.json"), ServiceAgentList.class);

		//System.err.printf("Results: %s\n", resultsUsingURL);
		assertNotNull(resultsUsingURL);
		assertEquals(3, resultsUsingURL.size());
		
		List<ServiceAgent> resultsUsingString = template.requestBody("direct:parseJSON", url("agents/example1/SNMP.json").toString(), ServiceAgentList.class);

		//System.err.printf("Results: %s\n", resultsUsingString);
		assertNotNull(resultsUsingString);
		assertEquals(3, resultsUsingString.size());
	}

	@Test
	public void testLoadCollectdConfiguration() throws Exception {
		
		template.requestBody("direct:loadCollectdConfiguration", null, String.class);
		
		SingletonBeanFactory<CollectdConfiguration> configSvc = bean("collectdConfigurationService", SingletonBeanFactory.class);
		
		assertNotNull(configSvc);
		assertNotNull(configSvc.getInstance());
		
		

	}
	@Test
	public void testLoadSnmpConfig() throws Exception {
		
		template.requestBody("direct:loadSnmpConfig", null, String.class);
		
		SingletonBeanFactory<SnmpConfiguration> configSvc = bean("snmpConfigurationService", SingletonBeanFactory.class);
		
		assertNotNull(configSvc);
		assertNotNull(configSvc.getInstance());

	}
	@Test
	public void testLoadDataCollectionConfig() throws Exception {
		
		template.requestBody("direct:loadDataCollectionConfig", null, String.class);
		
		SnmpMetricRepository metricRepo = bean("snmpMetricRepository", SnmpMetricRepository.class);
		
		assertNotNull(metricRepo);
		assertNotNull(metricRepo.getMetric("ifInOctets"));
		
	}
	@Test
	public void testLoadServiceAgents() throws Exception {
		
		PackageService svc = new PackageService("example1", "SNMP", 300000, "example1");
		
		List<ServiceAgent> agents = template.requestBody("direct:loadServiceAgents", svc, ServiceAgentList.class);
		
		assertNotNull(agents);
		assertEquals(3, agents.size());
		
	}
	
	@Test
	public void testLoadPackageServiceList() throws Exception {
		
		PackageService svc = new PackageService("example1", "SNMP", 300000, "example1");
		
		template.sendBody("seda:start", null);
		CollectdConfiguration collectConfig = template.requestBody("direct:collectdConfig", null, CollectdConfiguration.class);
		AgentList agents = template.requestBody("direct:loadPackageServiceList", collectConfig, AgentList.class);
		
		assertNotNull(agents);
		assertEquals(3, agents.size());
		
	}
	


	@Test(timeout=10000)
	public void testStartup() throws Exception {
		
		MockEndpoint result = getMockEndpoint("mock:result");
		result.expectedMessageCount(1);
		
		MockEndpoint scheduled = getMockEndpoint("mock:scheduled");
		scheduled.expectedMessageCount(2);
		
		//template.asyncRequestBody("seda:start", url("datacollection-config.xml"));
		//template.asyncRequestBody("seda:start", null);
		template.sendBody("seda:start", null);
		result.await();

		SingletonBeanFactory<CollectdConfiguration> collectdConfig = bean("collectdConfigurationService", SingletonBeanFactory.class);
		SingletonBeanFactory<SnmpConfiguration> snmpConfig = bean("snmpConfigurationService", SingletonBeanFactory.class);
		
		System.err.println("Waiting");

		//scheduled.await();
		
		System.err.println("Finished waiting");

		assertNotNull(collectdConfig);
		assertNotNull(collectdConfig.getInstance());

		assertNotNull(snmpConfig);
		assertNotNull(snmpConfig.getInstance());
	}

	private <T> T bean(String name,	Class<T> type) {
		return context().getRegistry().lookupByNameAndType(name, type);
	}
}
