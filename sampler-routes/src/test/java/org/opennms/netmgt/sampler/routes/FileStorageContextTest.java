package org.opennms.netmgt.sampler.routes;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;

import org.apache.camel.util.KeyValueHolder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opennms.core.test.camel.CamelBlueprintTest;
import org.opennms.netmgt.api.sample.SampleSetDispatcher;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

public class FileStorageContextTest extends CamelBlueprintTest {

	@BeforeClass
	public static void configureLogging() throws SecurityException, IOException {
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		lc.getLogger("org.apache.aries.blueprint").setLevel(Level.INFO);
	}

	// The location of our Blueprint XML file to be used for testing
	@Override
	protected String getBlueprintDescriptor() {
		return "file:blueprint-file-storage.xml";
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
