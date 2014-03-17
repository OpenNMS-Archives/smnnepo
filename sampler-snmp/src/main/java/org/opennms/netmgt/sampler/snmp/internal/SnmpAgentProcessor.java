package org.opennms.netmgt.sampler.snmp.internal;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.sampler.config.snmp.SnmpAgent;

/**
 * Wrap an {@link Agent} instance in a {@link SnmpAgent} instance. 
 */
public class SnmpAgentProcessor implements Processor {
	@Override
	public void process(Exchange exchange) throws Exception {
		Agent agent = exchange.getIn().getBody(Agent.class);
		exchange.getIn().setBody(new SnmpAgent(agent));
	}
}