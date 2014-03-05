package org.opennms.netmgt.sampler.config.snmp;

import java.net.URL;

import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.junit.Test;
import org.opennms.netmgt.snmp.SnmpConfiguration;

public class ConfigRouteTest extends CamelBlueprintTestSupport {

	@Override
	public boolean isUseAdviceWith() {
		return true;
	}

	// The location of our Blueprint XML file to be used for testing
	@Override
	protected String getBlueprintDescriptor() {
		return "file:routes-standalone.xml";
	}

	@Test
	public void testParseXML() throws Exception {
		context.start();

		SnmpConfiguration resultsUsingURL = template.requestBody("direct:parseXML", new URL("file:src/test/resources/snmp-config.xml"), SnmpConfiguration.class);

		//System.err.printf("Results: %s\n", resultsUsingURL);
		assertNotNull(resultsUsingURL);
		
		SnmpConfiguration resultsUsingString = template.requestBody("direct:parseXML", "file:src/test/resources/snmp-config.xml", SnmpConfiguration.class);

		//System.err.printf("Results: %s\n", resultsUsingString);
		assertNotNull(resultsUsingString);
	}

	@Override
	public boolean isUseDebugger() {
		// must enable debugger
		return true;
	}
}
