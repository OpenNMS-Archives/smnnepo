package org.opennms.netmgt.sampler.routes;

import java.io.IOException;
import java.util.Date;
import java.util.Dictionary;
import java.util.Map;

import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.util.KeyValueHolder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opennms.core.test.camel.CamelBlueprintTest;
import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.api.sample.SampleSetDispatcher;
import org.opennms.netmgt.api.sample.Timestamp;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

public class ActiveMQStorageContextTest extends CamelBlueprintTest {

	/**
	 * TODO: This isn't working properly because the ActiveMQ logs aren't getting routed
	 * through logback somehow.
	 */
	@BeforeClass
	public static void configureLogging() throws SecurityException, IOException {
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		lc.getLogger("org.apache.aries.blueprint").setLevel(Level.INFO);
		lc.getLogger("org.apache.activemq.broker.jmx").setLevel(Level.INFO);
	}

	// The location of our Blueprint XML file to be used for testing
	@Override
	protected String getBlueprintDescriptor() {
		return "file:blueprint-activemq-dispatch.xml";
	}

	@Override
	@SuppressWarnings("rawtypes")
	protected void addServicesOnStartup(Map<String, KeyValueHolder<Object, Dictionary>> services) {
		// Don't need any OSGi services yet
	}

	@Test(timeout=60000)
	public void test() throws Exception {

		assertTrue(context.hasEndpoint("mock:activemq:sampleSet") != null);
		assertTrue(context.hasEndpoint("direct:sendSampleSet") != null);
		assertTrue(context.hasEndpoint("mock:direct:sendSampleSet") != null);
		MockEndpoint endpoint = getMockEndpoint("mock:activemq:sampleSet", false);
		endpoint.setExpectedMessageCount(2);

		SampleSetDispatcher dispatcher = getOsgiService(SampleSetDispatcher.class);
		assertTrue(dispatcher != null);

		SampleSet set = new SampleSet(new Timestamp(new Date()));
		dispatcher.save(set);
		set = new SampleSet(new Timestamp(new Date()));
		dispatcher.save(set);

		assertMockEndpointsSatisfied();
	}
}
