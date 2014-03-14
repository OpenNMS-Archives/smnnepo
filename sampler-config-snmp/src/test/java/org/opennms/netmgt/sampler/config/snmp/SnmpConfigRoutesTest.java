package org.opennms.netmgt.sampler.config.snmp;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;

import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opennms.netmgt.api.sample.support.SingletonBeanFactory;
import org.opennms.netmgt.snmp.SnmpConfiguration;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

public class SnmpConfigRoutesTest extends CamelBlueprintTestSupport {

	private static final String OPENNMS_HOME = "src/test/resources";

	private static URL url(String path) throws MalformedURLException {
		return new URL("file:" + OPENNMS_HOME + "/" + path);
	}

	@BeforeClass
	public static void configureLogging() throws SecurityException, IOException {
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		lc.getLogger("org.apache.aries.blueprint").setLevel(Level.INFO);
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

	// The location of our Blueprint XML file to be used for testing
	@Override
	protected String getBlueprintDescriptor() {
		return "file:src/main/resources/OSGI-INF/blueprint/blueprint.xml";
	}

	/**
	 * Override 'opennms.home' with the test resource directory.
	 */
	@Override
	protected String useOverridePropertiesWithConfigAdmin(Dictionary props) throws Exception {
		props.put("opennms.home", OPENNMS_HOME);
		return "org.opennms.netmgt.sampler.config.snmp";
	}

	@Test
	public void testParseXML() throws Exception {
	        System.err.printf("Starting testParseXML");
		SnmpConfiguration resultsUsingURL = template.requestBody("direct:parseXML", new URL("file:" + OPENNMS_HOME + "/etc/snmp-config.xml"), SnmpConfiguration.class);

		System.err.printf("Results Using URL: %s\n", resultsUsingURL);
		assertNotNull(resultsUsingURL);

		SnmpConfiguration resultsUsingString = template.requestBody("direct:parseXML", "file:" + OPENNMS_HOME + "/etc/snmp-config.xml", SnmpConfiguration.class);

		System.err.printf("Results Using String: %s\n", resultsUsingString);
		assertNotNull(resultsUsingString);
	}

	@Test
	public void testLoadSnmpConfig() throws Exception {
            System.err.printf("Starting testLoadSnmpConfig");

            template.requestBody("direct:loadSnmpConfig", null, String.class);

		SingletonBeanFactory<SnmpConfiguration> configSvc = bean("snmpConfiguration", SingletonBeanFactory.class);

		System.err.printf("configSvc: %s\n", configSvc);
		assertNotNull(configSvc);
		assertNotNull(configSvc.getInstance());

	}

	/**
	 * Test loading the {@link SnmpMetricRepository}.
	 */
	@Test
	public void testLoadDataCollectionConfig() throws Exception {
	    System.err.printf("Starting testLoadDataCollectionConfig");
		template.requestBody("direct:loadDataCollectionConfig", null, String.class);

		SnmpMetricRepository metricRepo = bean("snmpMetricRepository", SnmpMetricRepository.class);

	            System.err.printf("metricRepo: %s\n", metricRepo);
		assertNotNull(metricRepo);
		assertNotNull(metricRepo.getMetric("ifInOctets"));

	}

	private <T> T bean(String name,	Class<T> type) {
		return context().getRegistry().lookupByNameAndType(name, type);
	}
}
