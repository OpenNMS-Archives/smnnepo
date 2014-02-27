package org.opennms.netmgt.sampler.snmp;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.xml.bind.JAXBContext;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Ignore;
import org.junit.Test;
import org.opennms.netmgt.sampler.config.snmp.SnmpMetricRepository;
import org.opennms.netmgt.sampler.snmp.CollectdConfigurationService.PackageAgentList;
import org.opennms.netmgt.sampler.snmp.CollectdConfigurationService.PackageService;
import org.opennms.netmgt.sampler.snmp.ServiceAgent.ServiceAgentList;

public class SamplerRoutingTest extends CamelTestSupport {
	
	public static final int AGENT_COUNT = 1;
	
	
	private static URL url(String path) throws MalformedURLException {
		return new URL("file:src/test/resources/" + path);
	}
	
	public static class Utils {
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
		
		registry.bind("collectdConfigurationService", new CollectdConfigurationService());;
		registry.bind("snmpConfigurationService", new SingletonBeanFactory<SnmpConfiguration>());;
		registry.bind("snmpMetricRepository", snmpMetricRepository);
		registry.bind("utils", new Utils());
		
		return registry;
	}
	

	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {

			@Override
			public void configure() throws Exception {
				
				JAXBContext context = JAXBContext.newInstance(CollectdConfiguration.class, SnmpConfiguration.class);

				JaxbDataFormat jaxb = new JaxbDataFormat(context);
				JacksonDataFormat  json = new JacksonDataFormat(ServiceAgentList.class);
				
				// Call this to retrieve a url in string form or URL form into the jaxb objects they represent
				from("direct:parseXML")
					.beanRef("utils", "toURL")
					.unmarshal(jaxb)
				;
				
				// Call this to retrieve a url in string form or URL form into the json objects they represent
				from("direct:parseJSON")
					.beanRef("utils", "toURL")
					.unmarshal(json)
				;
				
				from("seda:start")
				    // load all of the configs
					.multicast()
						.parallelProcessing().to("direct:loadCollectdConfiguration","direct:loadDataCollectionConfig","direct:loadSnmpConfig")
					.end()
					.log("==== Configuration Loaded. ===")
					.to("direct:schedulePackageServiceList")
					.to("mock:result")
				;

				from("direct:loadCollectdConfiguration")
					.transform(constant(url("collectd-configuration.xml")))
					.to("direct:parseXML")
					.beanRef("collectdConfigurationService", "setInstance")
				;

				from("direct:loadDataCollectionConfig")
					.log("Refreshing snmpMetricRepository")
					.beanRef("snmpMetricRepository", "refresh")
				;

				from("direct:loadSnmpConfig")
					.transform(constant(url("snmp-config.xml")))
					.to("direct:parseXML")
					.beanRef("snmpConfigurationService", "setInstance")
				;
				
				from("direct:loadPackageServiceList")
					.beanRef("collectdConfigurationService", "getPackageServiceList")
					.split().body()
						.log("PackageService: ${body}")
						.to("seda:loadPackageAgents")
					.end()
				;
				
				from("seda:processPackageService")
					.log("Processing Service: ${body}")
					.to("direct:loadServiceAgents")
					.to("seda:scheduleServiceAgents")
				;
				
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
					.beanRef("collectdConfigurationService", "setPackageAgentList")
					
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
		
		CollectdConfigurationService configSvc = bean("collectdConfigurationService", CollectdConfigurationService.class);
		
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
	@Ignore
	public void testStartup() throws Exception {
		
		MockEndpoint result = getMockEndpoint("mock:result");
		result.expectedMessageCount(1);
		
		MockEndpoint scheduled = getMockEndpoint("mock:scheduled");
		scheduled.expectedMessageCount(2);
		
		
		//template.asyncRequestBody("seda:start", url("datacollection-config.xml"));
		//template.asyncRequestBody("seda:start", null);
		template.sendBody("seda:start", null);

		CollectdConfigurationService configService = bean("collectdConfigurationService", CollectdConfigurationService.class);
		SingletonBeanFactory<SnmpConfiguration> snmpConfigurationService = bean("snmpConfigurationService", SingletonBeanFactory.class);
		
		System.err.println("Waiting");

		result.await();
		scheduled.await();
		
		System.err.println("Finished waiting");

		assertNotNull(configService);
		assertNotNull(configService.getInstance());

		assertNotNull(snmpConfigurationService);
		assertNotNull(snmpConfigurationService.getInstance());
		
	}


	private <T> T bean(String name,	Class<T> type) {
		return context().getRegistry().lookupByNameAndType(name, type);
	}


}
