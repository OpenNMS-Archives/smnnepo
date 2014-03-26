package org.opennms.netmgt.sampler.snmp.internal;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.support.SingletonBeanFactory;
import org.opennms.netmgt.config.snmp.AddressSnmpConfigVisitor;
import org.opennms.netmgt.config.snmp.Definition;
import org.opennms.netmgt.config.snmp.SnmpConfig;
import org.opennms.netmgt.sampler.config.snmp.SnmpAgent;

/**
 * Wrap an {@link Agent} instance in a {@link SnmpAgent} instance. 
 */
public class SnmpAgentProcessor implements Processor {
    SingletonBeanFactory<SnmpConfig> m_snmpConfigFactory;

    public SingletonBeanFactory<SnmpConfig> getSnmpConfigFactory() {
        return m_snmpConfigFactory;
    }
    
    public void setSnmpConfigFactory(final SingletonBeanFactory<SnmpConfig> factory) {
        m_snmpConfigFactory = factory;
    }

    @Override
    public void process(final Exchange exchange) throws Exception {
        final Agent agent = exchange.getIn().getBody(Agent.class);
        final SnmpAgent snmpAgent = new SnmpAgent(agent);

        if (m_snmpConfigFactory != null && m_snmpConfigFactory.getInstance() != null) {
            final SnmpConfig config = m_snmpConfigFactory.getInstance();
            final AddressSnmpConfigVisitor visitor = new AddressSnmpConfigVisitor(agent.getInetAddress());
            config.visit(visitor);
            final Definition def = visitor.getDefinition();
            
            snmpAgent.setCommunity(def.getReadCommunity());
            snmpAgent.setRetries(def.getRetry());
            snmpAgent.setTimeout(def.getTimeout());
            snmpAgent.setVersion(def.getVersion());
        }

        exchange.getIn().setBody(snmpAgent);
    }
}