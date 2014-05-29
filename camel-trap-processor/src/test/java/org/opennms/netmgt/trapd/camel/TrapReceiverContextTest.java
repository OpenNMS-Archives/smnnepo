package org.opennms.netmgt.trapd.camel;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Map;

import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.KeyValueHolder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpUtils;
import org.opennms.netmgt.snmp.SnmpV1TrapBuilder;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

public class TrapReceiverContextTest extends CamelBlueprintTestSupport {

	/**
	 * Use Aries Blueprint synchronous mode to avoid a blueprint
	 * deadlock bug.
	 * 
	 * @see https://issues.apache.org/jira/browse/ARIES-1051
	 * @see https://access.redhat.com/site/solutions/640943
	 */
	@Override
	public void doPreSetup() throws Exception { 
		System.setProperty("org.apache.aries.blueprint.synchronous", Boolean.TRUE.toString());
		System.setProperty("de.kalpatec.pojosr.framework.events.sync", Boolean.TRUE.toString());
	}

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
		return "file:src/main/resources/OSGI-INF/blueprint/blueprint-trap-receiver.xml";
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void addServicesOnStartup(Map<String, KeyValueHolder<Object, Dictionary>> services) {
		try {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test(timeout=60000)
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
		pdu.send(localhost, 162, "public");
		Thread.sleep(5000);

		assertMockEndpointsSatisfied();
	}
}
