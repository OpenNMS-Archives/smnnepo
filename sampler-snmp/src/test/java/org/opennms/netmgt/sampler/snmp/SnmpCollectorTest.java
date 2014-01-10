package org.opennms.netmgt.sampler.snmp;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Ignore;
import org.junit.Test;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.api.sample.Resource;
import org.opennms.netmgt.api.sample.Results;
import org.opennms.netmgt.api.sample.Results.Row;
import org.opennms.netmgt.api.sample.SampleRepository;
import org.opennms.netmgt.api.sample.Timestamp;
import org.opennms.netmgt.api.sample.support.SimpleFileRepository;
import org.opennms.netmgt.sampler.config.snmp.SnmpAgent;
import org.opennms.netmgt.sampler.config.snmp.SnmpMetricRepository;
import org.opennms.netmgt.sampler.snmp.internal.DefaultSnmpCollector;

public class SnmpCollectorTest extends CamelTestSupport {
	
	public static final int AGENT_COUNT = 1;
	
	
	public static class CollectionPackage {
		private final List<SnmpAgent> m_agents = new CopyOnWriteArrayList<SnmpAgent>();
		
		public void addAgent(SnmpAgent agent) {
			m_agents.add(agent);
		}
		
		public void removeAgent(SnmpAgent agent) {
			m_agents.remove(agent);
		}
		
		public List<SnmpAgent> getAgents() {
			return m_agents;
		}
	}
	
	private static URL url(String path) throws MalformedURLException {
		return new URL("file:../configuration-snmp/src/main/resources/" + path);
	}
	

	@Override
	protected JndiRegistry createRegistry() throws Exception {
		JndiRegistry registry = super.createRegistry();
		
		SnmpMetricRepository repository = new SnmpMetricRepository(
				url("datacollection-config.xml"), 
				url("datacollection/mib2.xml"), 
				url("datacollection/netsnmp.xml"),
				url("datacollection/dell.xml")
				);
		
		SimpleFileRepository sampleRepository = new SimpleFileRepository(
				new File("target/attributes.properties"),
				new File("target/samples.txt"));
		
		registry.bind("package", new CollectionPackage());
		registry.bind("snmpMetricRepository", repository);
		registry.bind("sampleRepository", sampleRepository);
		
		
		return registry;
	}

	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {

			@Override
			public void configure() throws Exception {
				
				// add agents to the package
				from("seda:add")
				.beanRef("package", "addAgent")
				.log("Agent '${body}' added");
				
				// every collection interval (1s)
				//from("timer://collectTimer?fixedRate=true&period=1s&delay=3000")
				//.to("seda:doCollection");
				
				from("seda:doCollection")
				// get the list of agents whose data needs to be collected
				.beanRef("package", "getAgents")
				// for each agent
				.split(body())
					// collect data for the agent
					.to("seda:collectAgents")
				.end()
				.log("Finished requesting collection ${property.CamelTimerCounter}.");
					
				
				from("seda:collectAgents")
				// create a request for data collection for the agent
				.beanRef("snmpMetricRepository", "createRequestForAgent")
				// collect the data for the agent
				.bean(DefaultSnmpCollector.class, "collect")
				// forward the data to the listening queue
				.log("${body}")
				.to("seda:sampleSets");

				
				// for each SampleSet
				from("seda:sampleSets")
				// send it the persister
				.beanRef("sampleRepository", "save")
				.to("log:samples?groupSize=" + AGENT_COUNT)
				.to("mock:sampleSaved")
				;
			}
			
		};
	}

	@Test
	@Ignore
	public void test() throws InterruptedException {
		for(int i = 1; i <= AGENT_COUNT; i++) {
			SnmpAgent agent = new SnmpAgent(new InetSocketAddress("10.0.1."+i, 161), ".1.3.6.1.4.1.8072.3.2.255");
			agent.setCommunity("public");
			template.sendBody("seda:add", agent);
		}
		
		
		MockEndpoint sampleSaved = getMockEndpoint("mock:sampleSaved");
		sampleSaved.expectedMessageCount(1);
		
		Timestamp start = Timestamp.now();

		template.sendBodyAndHeader("seda:doCollection", "", "CamelTimerCounter", 1);
		
		MockEndpoint.assertIsSatisfied(sampleSaved);
		
		Timestamp end = Timestamp.now();
		
		SampleRepository repository = context().getRegistry().lookup("sampleRepository", SampleRepository.class);
		SnmpMetricRepository metricRepo = context().getRegistry().lookup("snmpMetricRepository", SnmpMetricRepository.class);
		
		Resource resource = new Resource(new Agent(new InetSocketAddress("10.0.1.1", 161), "snmp"), "dskIndex", "/");
		Set<Metric> metricSet = metricRepo.getMetrics("net-snmp-disk");
		assertNotNull(metricSet);
		Metric[] metrics = metricSet.toArray(new Metric[0]);
		assertNotNull(metrics);
		assertEquals(4, metrics.length);
		
		
		Results results = repository.find(null, start, end, resource, metrics);
		assertNotNull(results);
		System.err.println("RESULTS: " + results);
		Collection<Row> rows = results.getRows();
		assertNotNull(rows);
		assertEquals(1, rows.size());
		
		
	}


}
