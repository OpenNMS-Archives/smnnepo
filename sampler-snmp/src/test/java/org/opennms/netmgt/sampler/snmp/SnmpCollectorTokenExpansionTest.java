package org.opennms.netmgt.sampler.snmp;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.util.KeyValueHolder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.TestContextAware;
import org.opennms.core.test.TestContextAwareExecutionListener;
import org.opennms.core.test.camel.CamelBlueprintTest;
import org.opennms.core.test.snmp.JUnitSnmpAgentExecutionListener;
import org.opennms.core.test.snmp.annotations.JUnitSnmpAgent;
import org.opennms.core.xml.JaxbUtils;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.CollectionConfiguration;
import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.api.sample.Resource;
import org.opennms.netmgt.api.sample.Results;
import org.opennms.netmgt.api.sample.Results.Row;
import org.opennms.netmgt.api.sample.Sample;
import org.opennms.netmgt.api.sample.SampleRepository;
import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.api.sample.SampleSetDispatcher;
import org.opennms.netmgt.api.sample.support.SimpleFileRepository;
import org.opennms.netmgt.api.sample.support.SingletonBeanFactory;
import org.opennms.netmgt.api.sample.support.SingletonBeanFactoryImpl;
import org.opennms.netmgt.collection.api.CollectionResource;
import org.opennms.netmgt.collection.sampler.SamplerCollectionResource;
import org.opennms.netmgt.config.snmp.SnmpConfig;
import org.opennms.netmgt.rrd.RrdRepository;
import org.opennms.netmgt.sampler.config.snmp.SnmpAgent;
import org.opennms.netmgt.sampler.config.snmp.SnmpMetricRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({
	TestContextAwareExecutionListener.class,
	JUnitSnmpAgentExecutionListener.class
})
@ContextConfiguration(locations={
	"classpath:/snmpCollectorTest-context.xml"
})
public class SnmpCollectorTokenExpansionTest extends CamelBlueprintTest implements TestContextAware {
	
	private static final Logger LOG = LoggerFactory.getLogger(SnmpCollectorTokenExpansionTest.class);

	public static final int AGENT_COUNT = 1;
	
	private TestContext m_testContext;
	private SampleRepository m_sampleRepository;
	private CountDownLatch m_latch = null;
	private SampleSet m_sampleSet = null;
	private SnmpMetricRepository m_snmpMetricRepository;

	@Override
	public void setTestContext(TestContext testContext) {
		m_testContext = testContext;
	}

	@BeforeClass
	public static void configureLogging() throws SecurityException, IOException {
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		lc.getLogger("org.apache.aries.blueprint").setLevel(Level.INFO);
	}

	@Override
	protected String getBundleFilter() {
		// Don't start the config or config-snmp bundles
		return "(& (!(Bundle-SymbolicName=org.opennms.netmgt.sampler.config)) (!(Bundle-SymbolicName=org.opennms.netmgt.sampler.config.snmp)) )";
	}

	// The location of our Blueprint XML file to be used for testing
	@Override
	protected String getBlueprintDescriptor() {
		return "file:src/main/resources/OSGI-INF/blueprint/blueprint.xml";
	}

	/**
	 * This class will use a countdown latch every time save() is called.
	 */
	private class CountDownLatchSimpleFileRepository extends SimpleFileRepository {
		public CountDownLatchSimpleFileRepository(File attributesFile, File sampleFile) {
			super(attributesFile, sampleFile);
		}

		@Override
		public void save(SampleSet samples) {
			super.save(samples);
			m_sampleSet = samples;
			System.err.println("Called SampleRepository.save()");
			if (m_latch != null) {
				m_latch.countDown();
			}
		}
	}

	/**
	 * Register a mock OSGi {@link SampleRepository}.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	protected void addServicesOnStartup(Map<String, KeyValueHolder<Object, Dictionary>> services) {
		try {

			// Delete the test files after the test completes
			new File("target/attributes.properties").delete();
			new File("target/samples.txt").delete();

			m_sampleRepository = new CountDownLatchSimpleFileRepository(
				new File("target/attributes.properties"),
				new File("target/samples.txt")
			);

			m_snmpMetricRepository = new SnmpMetricRepository(
				// Use a full copy of the config as fetched from:
				// http://[opennms]:8980/opennms/rest/config/datacollection
				//
				new URL("file:src/test/resources/full-datacollection-config.xml"),
				new URL[0]
			); 

			SnmpConfig snmpConfig = JaxbUtils.unmarshal(SnmpConfig.class, new File("src/test/resources/snmp-config.xml"));

			services.put(SampleSetDispatcher.class.getName(), new KeyValueHolder<Object,Dictionary>(m_sampleRepository, new Properties()));
			services.put(CollectionConfiguration.class.getName(), new KeyValueHolder<Object,Dictionary>(
				m_snmpMetricRepository,
				new Properties())
			);
			Properties props = new Properties();
			props.put("beanClass", "org.opennms.netmgt.config.snmp.SnmpConfig");
			services.put(SingletonBeanFactory.class.getName(), new KeyValueHolder<Object,Dictionary>(new SingletonBeanFactoryImpl<SnmpConfig>(snmpConfig), props));

		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	// 172.20.1.7 sysOid:  .1.3.6.1.4.1.4526.100.10.7
	// 172.20.1.23 sysOid: .1.3.6.1.4.1.8072.3.2.10
	//@Test(timeout=60000)
	@Test
	@JUnitSnmpAgent(resource="classpath:172.20.1.7.mib2-powerethernet.snmpwalk")
	public void testTokenExpansion() throws Exception {
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

		// We should get one call to {@link SampleRepository#save(SampleSet)}
		m_latch = new CountDownLatch(1);

		MockEndpoint sampleSaved = getMockEndpoint("mock:seda:saveToRepository", false);
		sampleSaved.expectedMessageCount(1);

		// Make some constants to reuse in the regex below
		final String baseDir = "TEST";
		final String nodeId = "1";

		for(int i = 1; i <= AGENT_COUNT; i++) {
			Agent agent = new Agent(
				new InetSocketAddress(
					(InetAddress)m_testContext.getAttribute(JUnitSnmpAgentExecutionListener.IPADDRESS_KEY),
                                        (Integer)m_testContext.getAttribute(JUnitSnmpAgentExecutionListener.PORT_KEY)
				),
				"SNMP",
				String.valueOf(i)
			);
			agent.setParameter(SnmpAgent.PARAM_SYSOBJECTID, ".1.3.6.1.4.1.4526.100.10.7");
			agent.setParameter(SnmpAgent.PARAM_COMMUNITY, "public");
			agent.setParameter("nodeId", nodeId);
			template.sendBody("seda:collectAgent", agent);
		}

		MockEndpoint.assertIsSatisfied(sampleSaved);
		m_latch.await();

		RrdRepository repo = new RrdRepository();
		repo.setRrdBaseDir(new File(baseDir));

		assertTrue(m_sampleSet != null);
		LOG.debug(m_sampleSet.toString());
		for (Resource resource : m_sampleSet.getResources()) {
			CollectionResource collectionResource = new SamplerCollectionResource(resource, repo);
			Path resourcePath = collectionResource.getPath();
			LOG.debug("Resource directory: {}", resourcePath);
			// We should get indices 1 through 6
			assertTrue(resourcePath.toString(), resourcePath.toString().matches("^" + nodeId + "/pethMainPseGroupIndex/[1-6]$"));
		}

		Set<Metric> metricSet = m_snmpMetricRepository.getMetrics("mib2-powerethernet");
		assertNotNull(metricSet);
		Metric[] metrics = metricSet.toArray(new Metric[0]);
		assertNotNull(metrics);
		assertEquals(2, metrics.length);

		Resource resource = new Resource(
			new Agent(
				new InetSocketAddress(
					(InetAddress)m_testContext.getAttribute(JUnitSnmpAgentExecutionListener.IPADDRESS_KEY),
					1161
				),
				"SNMP",
				"1"
			),
			"pethMainPseGroupIndex",
			// This is the focus of this test... to ensure that the resourceName and
			// resourceLabel template values of ${index} are being expanded properly.
			"1", 
			"1"
		);

		// Retrieve all results from the repository that match the resource and metrics
		Results results = m_sampleRepository.find(null, null, null, resource, metrics);
		assertNotNull(results);
		LOG.info("RESULTS: " + results);
		Collection<Row> rows = results.getRows();
		assertNotNull(rows);
		assertEquals(1, rows.size());
		
		// Check to make sure that the collected values match the mock SNMP values
		Iterator<Sample> samples = rows.iterator().next().iterator();
		Sample sample = samples.next();
		assertEquals("pethMainPseConPower", sample.getMetric().getName());
		assertEquals(19, sample.getValue().intValue());
		sample = samples.next();
		assertEquals("pethMainPsePower", sample.getMetric().getName());
		assertEquals(384, sample.getValue().intValue());

		resource = new Resource(
			new Agent(
				new InetSocketAddress(
					(InetAddress)m_testContext.getAttribute(JUnitSnmpAgentExecutionListener.IPADDRESS_KEY),
					1161
				),
				"SNMP",
				"1"
			),
			"pethMainPseGroupIndex",
			// This is the focus of this test... to ensure that the resourceName and
			// resourceLabel template values of ${index} are being expanded properly.
			"3", 
			"3"
		);

		// Retrieve all results from the repository that match the resource and metrics
		results = m_sampleRepository.find(null, null, null, resource, metrics);
		assertNotNull(results);
		LOG.info("RESULTS: " + results);
		rows = results.getRows();
		assertNotNull(rows);
		assertEquals(1, rows.size());
		
		// Check to make sure that the collected values match the mock SNMP values
		samples = rows.iterator().next().iterator();
		sample = samples.next();
		assertEquals("pethMainPseConPower", sample.getMetric().getName());
		assertEquals(0, sample.getValue().intValue());
		sample = samples.next();
		assertEquals("pethMainPsePower", sample.getMetric().getName());
		assertEquals(1, sample.getValue().intValue());

		resource = new Resource(
			new Agent(
				new InetSocketAddress(
					(InetAddress)m_testContext.getAttribute(JUnitSnmpAgentExecutionListener.IPADDRESS_KEY),
					1161
				),
				"SNMP",
				"1"
			),
			"pethMainPseGroupIndex",
			// This is the focus of this test... to ensure that the resourceName and
			// resourceLabel template values of ${index} are being expanded properly.
			"6", 
			"6"
		);

		// Retrieve all results from the repository that match the resource and metrics
		results = m_sampleRepository.find(null, null, null, resource, metrics);
		assertNotNull(results);
		LOG.info("RESULTS: " + results);
		rows = results.getRows();
		assertNotNull(rows);
		assertEquals(1, rows.size());
		
		// Check to make sure that the collected values match the mock SNMP values
		samples = rows.iterator().next().iterator();
		sample = samples.next();
		assertEquals("pethMainPseConPower", sample.getMetric().getName());
		assertEquals(0, sample.getValue().intValue());
		sample = samples.next();
		assertEquals("pethMainPsePower", sample.getMetric().getName());
		assertEquals(1, sample.getValue().intValue());
	}
}
