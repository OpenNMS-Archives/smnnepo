package org.opennms.netmgt.trapd.camel;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.util.KeyValueHolder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opennms.core.test.camel.CamelBlueprintTest;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpUtils;
import org.opennms.netmgt.snmp.SnmpV1TrapBuilder;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

public class TrapReceiverContextTest extends CamelBlueprintTest {

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

	@Override
	protected String setConfigAdminInitialConfiguration(Properties props) {
		props.put("trapListenAddress", "127.0.0.1");
		props.put("trapListenPort", "9162");
		return "org.opennms.netmgt.sampler.trapReceiver";
	}

	/**
	 * The location of our Blueprint XML file to be used for testing.
	 */
	@Override
	protected String getBlueprintDescriptor() {
		return "file:src/main/resources/OSGI-INF/blueprint/blueprint-trap-receiver.xml";
	}

	@Override
	@SuppressWarnings("rawtypes")
	protected void addServicesOnStartup(Map<String, KeyValueHolder<Object, Dictionary>> services) {
		// Don't need any OSGi services yet
	}

	@Test(timeout=60000)
	public void test() throws Exception {

		assertTrue(context.hasEndpoint("mock:activemq:snmpTrap") != null);
		MockEndpoint endpoint = getMockEndpoint("mock:activemq:snmpTrap", false);
		endpoint.setExpectedMessageCount(1);

		// Send a trap
		String localhost = "127.0.0.1";
		InetAddress localAddr = InetAddressUtils.addr(localhost);
		SnmpV1TrapBuilder pdu = SnmpUtils.getV1TrapBuilder();
		pdu.setEnterprise(SnmpObjId.get(".1.3.6.1.4.1.5813"));
		pdu.setGeneric(1);
		pdu.setSpecific(0);
		pdu.setTimeStamp(666L);
		pdu.setAgentAddress(localAddr);
		pdu.send(localhost, 9162, "public");
		Thread.sleep(5000);

		assertMockEndpointsSatisfied();
	}
}
