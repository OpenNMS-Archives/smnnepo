package org.opennms.netmgt.sampler.snmp;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Future;

import javax.xml.bind.JAXBContext;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.NoTypeConversionAvailableException;
import org.apache.camel.Processor;
import org.apache.camel.TypeConversionException;
import org.apache.camel.TypeConverter;
import org.apache.camel.WrappedFile;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.bean.BeanInvocation;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.support.TypeConverterSupport;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.opennms.netmgt.api.sample.support.SimpleFileRepository;
import org.opennms.netmgt.sampler.config.snmp.SnmpMetricRepository;
import org.opennms.netmgt.sampler.snmp.CollectdConfigurationService.CollectionService;
import org.opennms.netmgt.sampler.snmp.ServiceAgent.ServiceAgentList;

public class SamplerRoutingTest extends CamelTestSupport {
	
	public static final int AGENT_COUNT = 1;
	
	
	private static URL url(String path) throws MalformedURLException {
		return new URL("file:src/test/resources/" + path);
	}
	
	public static class Utils {
		public URL stringToURL(String s) throws MalformedURLException {
			return new URL(s);
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
		
		SimpleFileRepository sampleRepository = new SimpleFileRepository(
				new File("target/attributes.properties"),
				new File("target/samples.txt"));
		
		
		
		CollectdConfigurationService collectdConfigurationService = new CollectdConfigurationService();
		
		SnmpConfigurationService snmpConfigurationService = new SnmpConfigurationService();
		
		registry.bind("collectdConfigurationService", collectdConfigurationService);;
		registry.bind("snmpConfigurationService", snmpConfigurationService);;
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
				
				from("seda:start")
				.multicast()
					.parallelProcessing().to("direct:loadCollectdConfiguration","direct:loadDataCollectionConfig","direct:loadSnmpConfig")
				.end()
				.log("==== Configuration Loaded. ===")
				.beanRef("collectdConfigurationService", "getServiceList")
				.split().body()
					.log("CollectionService: ${body}")
					.to("seda:processService")
				.end()
				.to("mock:result")
				;

				from("direct:loadCollectdConfiguration")
				.transform(constant(url("collectd-configuration.xml")))
				.log("Loading collectd-configuration.xml:\n ${body}")
				.unmarshal(jaxb)
				.beanRef("collectdConfigurationService", "setConfiguration")
				;

				from("direct:loadDataCollectionConfig")
				.log("Refreshing snmpMetricRepository")
				.beanRef("snmpMetricRepository", "refresh")
				;

				from("direct:loadSnmpConfig")
				.transform(constant(url("snmp-config.xml")))
				.log("Loading snmp-config.xml:\n ${body}")
				.unmarshal(jaxb)
				.beanRef("snmpConfigurationService", "setConfiguration")
				;
				
				from("seda:processService")
				.log("Processing Service: ${body}")
				.to("direct:loadServiceAgents")
				.to("seda:scheduleServiceAgents")
				;

				from("direct:loadServiceAgents")
				.transform().simple("file:src/test/resources/agents/${body.filterName}/${body.svcName}.json")
				.beanRef("utils", "stringToURL")
				.log("BODY IS: ${body}")
				.unmarshal(json)
				.log("${body}")
				.to("mock:loaded");

			}
			
		};
	}

	@Test
	public void testLoadService() throws Exception {
		
		MockEndpoint loaded = getMockEndpoint("mock:loaded");
		loaded.expectedMessageCount(1);

		CollectionService svc = new CollectionService("example1", "SNMP", 300000, "example1");
		
		template.sendBody("direct:loadServiceAgents", svc);

		loaded.await();
		
		System.err.println("Finished waiting");

		
	}


	@Test
	public void testStartup() throws Exception {
		
		MockEndpoint result = getMockEndpoint("mock:result");
		result.expectedMessageCount(1);
		
		MockEndpoint scheduled = getMockEndpoint("mock:scheduled");
		scheduled.expectedMessageCount(2);
		
		
		//template.asyncRequestBody("seda:start", url("datacollection-config.xml"));
		//template.asyncRequestBody("seda:start", null);
		template.sendBody("seda:start", null);

		CollectdConfigurationService configService = context().getRegistry().lookupByNameAndType("collectdConfigurationService", CollectdConfigurationService.class);
		SnmpConfigurationService snmpConfigurationService = context().getRegistry().lookupByNameAndType("snmpConfigurationService", SnmpConfigurationService.class);
		
		System.err.println("Waiting");

		result.await();
		scheduled.await();
		
		System.err.println("Finished waiting");

		assertNotNull(configService);
		assertNotNull(configService.getConfiguration());

		assertNotNull(snmpConfigurationService);
		assertNotNull(snmpConfigurationService.getConfiguration());
		
	}


}
