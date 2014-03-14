package org.opennms.netmgt.sampler.snmp;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.TestContextAware;
import org.opennms.core.test.TestContextAwareExecutionListener;
import org.opennms.core.test.snmp.JUnitSnmpAgentExecutionListener;
import org.opennms.core.test.snmp.annotations.JUnitSnmpAgent;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({
	TestContextAwareExecutionListener.class,
	JUnitSnmpAgentExecutionListener.class
})
@ContextConfiguration(locations={
	"classpath:/snmpCollectorTest-context.xml"
})
public class SnmpCollectorTest extends CamelTestSupport implements TestContextAware {
	
	private static final Logger LOG = LoggerFactory.getLogger(SnmpCollectorTest.class);

	public static final int AGENT_COUNT = 1;
	
	private TestContext m_testContext;

	@Override
	public void setTestContext(TestContext testContext) {
		m_testContext = testContext;
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

	private static URL url(String path) throws MalformedURLException {
		return new URL("file:../sampler-config-snmp/src/test/resources/etc/" + path);
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

		// Delete the test files after the test completes
		new File("target/attributes.properties").delete();
		new File("target/samples.txt").delete();

		SimpleFileRepository sampleRepository = new SimpleFileRepository(
			new File("target/attributes.properties"),
			new File("target/samples.txt")
		);

		registry.bind("snmpMetricRepository", repository);
		registry.bind("sampleRepository", sampleRepository);

		return registry;
	}

	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {

			@Override
			public void configure() throws Exception {

				from("seda:collectAgent")
					// Convert the generic Agent into an SnmpAgent
					.bean(SnmpAgentProcessor.class)
					// create a request for data collection for the agent
					.beanRef("snmpMetricRepository", "createRequestForAgent")
					// collect the data for the agent
					.bean(DefaultSnmpCollector.class, "collect")
					// forward the data to the listening queue
					.log("${body}")
					.to("seda:sampleSets")
				;

				// for each SampleSet
				from("seda:sampleSets")
					// send it the persister
					.beanRef("sampleRepository", "save")
					.to("log:samples?groupSize=" + AGENT_COUNT)
					.to("seda:sampleSaved")
				;
			}
		};
	}

	/**
	 * Wrap an {@link Agent} instance in a {@link SnmpAgent} instance. 
	 */
	public static class SnmpAgentProcessor implements Processor {
		@Override
		public void process(Exchange exchange) throws Exception {
			Agent agent = exchange.getIn().getBody(Agent.class);
			exchange.getIn().setBody(new SnmpAgent(agent));
		}
	}

	@Test
	@JUnitSnmpAgent(resource="classpath:laptop.properties")
	public void test() throws Exception {
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

		MockEndpoint sampleSaved = getMockEndpoint("mock:seda:sampleSaved");
		sampleSaved.expectedMessageCount(1);

		Timestamp start = Timestamp.now();

		for(int i = 1; i <= AGENT_COUNT; i++) {
			Agent agent = new Agent(
				new InetSocketAddress(
					(InetAddress)m_testContext.getAttribute(JUnitSnmpAgentExecutionListener.IPADDRESS_KEY),
					1161
				),
				"SNMP",
				String.valueOf(i)
			);
			agent.setParameter(SnmpAgent.PARAM_SYSOBJECTID, ".1.3.6.1.4.1.8072.3.2.255");
			agent.setParameter(SnmpAgent.PARAM_COMMUNITY, "public");
			template.sendBody("seda:collectAgent", agent);
		}

		MockEndpoint.assertIsSatisfied(sampleSaved);

		Timestamp end = Timestamp.now();

		SampleRepository repository = context().getRegistry().lookupByNameAndType("sampleRepository", SampleRepository.class);
		SnmpMetricRepository metricRepo = context().getRegistry().lookupByNameAndType("snmpMetricRepository", SnmpMetricRepository.class);

		Set<Metric> metricSet = metricRepo.getMetrics("ucd-loadavg");
		assertNotNull(metricSet);
		Metric[] metrics = metricSet.toArray(new Metric[0]);
		assertNotNull(metrics);
		assertEquals(3, metrics.length);

		Resource resource = new Resource(
			new Agent(
				new InetSocketAddress(
					(InetAddress)m_testContext.getAttribute(JUnitSnmpAgentExecutionListener.IPADDRESS_KEY),
					1161
				),
				"SNMP",
				"1"
			),
			"node",
			"ucd-loadavg"
		);

		// Retrieve all results from the repository that match the resource and metrics
		Results results = repository.find(null, null, null, resource, metrics);
		assertNotNull(results);
		LOG.info("RESULTS: " + results);
		Collection<Row> rows = results.getRows();
		assertNotNull(rows);
		assertEquals(1, rows.size());
	}
}
