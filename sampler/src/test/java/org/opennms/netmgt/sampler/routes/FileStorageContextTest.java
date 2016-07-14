package org.opennms.netmgt.sampler.routes;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.util.KeyValueHolder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opennms.core.test.MockLogAppender;
import org.opennms.core.test.camel.CamelBlueprintTest;
import org.opennms.netmgt.api.sample.SampleSetDispatcher;

public class FileStorageContextTest extends CamelBlueprintTest {

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
		return "file:src/main/blueprint/blueprint-sampler-storage-file.xml";
	}

	@Override
	@SuppressWarnings("rawtypes")
	protected void addServicesOnStartup(Map<String, KeyValueHolder<Object, Dictionary>> services) {
		// Don't need any OSGi services yet
	}

	@Test(timeout=60000)
	public void test() throws Exception {

		assertTrue(getOsgiService(SampleSetDispatcher.class) != null);

	}
}
