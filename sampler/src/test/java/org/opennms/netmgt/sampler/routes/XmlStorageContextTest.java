package org.opennms.netmgt.sampler.routes;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.JAXBContext;

import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.util.KeyValueHolder;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opennms.core.test.MockLogAppender;
import org.opennms.core.test.camel.CamelBlueprintTest;
import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.api.sample.SampleSetDispatcher;
import org.opennms.netmgt.api.sample.Timestamp;

public class XmlStorageContextTest extends CamelBlueprintTest {

	/**
	 * TODO: This isn't working properly.
	 */
	@BeforeClass
	public static void configureLogging() throws SecurityException, IOException {
		Properties props = new Properties();
		props.setProperty("org.apache.aries.blueprint", "INFO");
		props.setProperty("org.apache.activemq.broker.jmx", "INFO");
		props.setProperty("org.apache.camel.impl.osgi", "INFO");
		MockLogAppender.setupLogging(props);
	}

	// The location of our Blueprint XML file to be used for testing
	@Override
	protected String getBlueprintDescriptor() {
		return "file:src/main/blueprint/blueprint-sampler-storage-xml.xml";
	}

	@Override
	@SuppressWarnings("rawtypes")
	protected void addServicesOnStartup(Map<String, KeyValueHolder<Object, Dictionary>> services) {
		// Don't need any OSGi services yet
	}

	@Test(timeout=60000)
	@Ignore // Only used to sanity check the XML context filename Camel code
	public void test() throws Exception {

		assertTrue(context.hasEndpoint("mock:file:xml") != null);
		MockEndpoint endpoint = getMockEndpoint("mock:file:xml", false);
		// Only one of the SampleSets will make it through because only one has sample values in it
		endpoint.setExpectedMessageCount(1);

		SampleSetDispatcher dispatcher = getOsgiService(SampleSetDispatcher.class);
		assertTrue(dispatcher != null);

		// Unmarshal the SampleSet from a file
		SampleSet set = (SampleSet)JAXBContext.newInstance(SampleSet.class).createUnmarshaller().unmarshal(new File("src/test/resources/sampleSet.xml"));
		dispatcher.save(set);
		set = new SampleSet(new Timestamp(new Date()));
		dispatcher.save(set);

		assertMockEndpointsSatisfied();
	}
}
