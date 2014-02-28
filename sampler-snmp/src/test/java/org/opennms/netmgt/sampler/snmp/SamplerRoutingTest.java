package org.opennms.netmgt.sampler.snmp;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBContext;

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
import org.opennms.netmgt.api.sample.support.SingletonBeanFactory;
import org.opennms.netmgt.config.collectd.CollectdConfiguration;
import org.opennms.netmgt.config.collectd.Package;
import org.opennms.netmgt.config.collectd.Service;
import org.opennms.netmgt.sampler.config.snmp.SnmpMetricRepository;
import org.opennms.netmgt.sampler.snmp.ServiceAgent.ServiceAgentList;

public class SamplerRoutingTest extends CamelTestSupport {
	
	public static final int AGENT_COUNT = 1;
	
	private static URL url(String path) throws MalformedURLException {
		return new URL("file:src/test/resources/" + path);
	}

	private static class AgentList extends ArrayList<ServiceAgent> {}

	/**
	 * This class will convert all incoming objects to URLs.
	 */
	public static class UrlNormalizer {

		public URL toURL(String s) throws MalformedURLException {
			return new URL(s);
		}

		public URL toURL(URL u) {
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
		
		registry.bind("collectdConfiguration", new SingletonBeanFactory<CollectdConfiguration>());
		registry.bind("snmpConfiguration", new SingletonBeanFactory<SnmpConfiguration>());;
		registry.bind("snmpMetricRepository", snmpMetricRepository);
		registry.bind("urlNormalizer", new UrlNormalizer());
		
		return registry;
	}
	

	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {

			@Override
			public void configure() throws Exception {
				
				onException(IOException.class)
					.handled(true)
					//.transform().constant(null)
					.stop()
				;
				
				JAXBContext context = JAXBContext.newInstance(CollectdConfiguration.class, SnmpConfiguration.class);

				JaxbDataFormat jaxb = new JaxbDataFormat(context);
				JacksonDataFormat json = new JacksonDataFormat(ServiceAgentList.class);
				//JacksonDataFormat json = new JacksonDataFormat();
				
				// Call this to retrieve a URL in string form or URL form into the JAXB objects they represent
				from("direct:parseXML")
					.beanRef("urlNormalizer")
					.unmarshal(jaxb)
				;
				
				// Call this to retrieve a URL in string form or URL form into the JSON objects they represent
				from("direct:parseJSON")
					.beanRef("urlNormalizer")
					.unmarshal(json)
				;

				// Direct route to fetch the config
				from("direct:collectdConfig")
					.beanRef("collectdConfiguration", "getInstance")
				;

				// Direct route to fetch the config
				from("direct:snmpConfig")
					.beanRef("snmpConfiguration", "getInstance")
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
					.to("mock:result")
				;

				// TODO: Create a reload timer that will check for changes to the config
				from("direct:loadCollectdConfiguration")
					.transform(constant(url("collectd-configuration.xml")))
					.to("direct:parseXML")
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
					.to("direct:parseXML")
					.beanRef("snmpConfiguration", "setInstance")
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
							CollectdConfiguration config = exchange.getIn().getBody(CollectdConfiguration.class);
							List<PackageService> retval = new ArrayList<PackageService>();
							for (org.opennms.netmgt.config.collectd.Package pkg : config.getPackages()) {
								for (Service svc : pkg.getServices()) {
									retval.add(new PackageService(pkg.getName(), svc));
								}
							}
							exchange.getIn().setBody(retval);
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
								.beanRef("collectdConfiguration", "getPackageServiceList")
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
					//.beanRef("collectdConfiguration", "setPackageAgentList")
				;

				from("direct:getServiceAgents")
					.transform().simple("file:src/test/resources/agents/${body.packageName}/${body.service.name}.json")
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
		
		SingletonBeanFactory<CollectdConfiguration> configSvc = bean("collectdConfiguration", SingletonBeanFactory.class);
		
		assertNotNull(configSvc);
		assertNotNull(configSvc.getInstance());
		
		

	}
	@Test
	public void testLoadSnmpConfig() throws Exception {
		
		template.requestBody("direct:loadSnmpConfig", null, String.class);
		
		SingletonBeanFactory<SnmpConfiguration> configSvc = bean("snmpConfiguration", SingletonBeanFactory.class);
		
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
		
		Service svc = new Service();
		svc.setInterval(1000L);
		svc.setName("SNMP");
		svc.setStatus("on");
		svc.setUserDefined("false");

		Package pkg = new Package();
		pkg.setName("package1");
		pkg.setServices(Collections.singletonList(svc));
		
		PackageService packSvc = new PackageService("package1", svc);
		
		List<ServiceAgent> agents = template.requestBody("direct:loadServiceAgents", packSvc, ServiceAgentList.class);
		
		assertNotNull(agents);
		assertEquals(3, agents.size());
		
	}
	
	@Test
	public void testLoadPackageServiceList() throws Exception {
		
		// Load the CollectdConfiguration
		template.sendBody("direct:loadCollectdConfiguration", null);
		
		// Fetch the loaded config
		CollectdConfiguration collectConfig = template.requestBody("direct:collectdConfig", null, CollectdConfiguration.class);
		// Pass it to the method that parses the config into a package/service object
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

		SingletonBeanFactory<CollectdConfiguration> collectdConfig = bean("collectdConfiguration", SingletonBeanFactory.class);
		SingletonBeanFactory<SnmpConfiguration> snmpConfig = bean("snmpConfiguration", SingletonBeanFactory.class);
		
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
