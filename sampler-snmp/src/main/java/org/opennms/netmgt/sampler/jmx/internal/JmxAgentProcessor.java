package org.opennms.netmgt.sampler.jmx.internal;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.support.SingletonBeanFactory;
import org.opennms.netmgt.config.snmp.AddressSnmpConfigVisitor;
import org.opennms.netmgt.config.snmp.Definition;
import org.opennms.netmgt.config.snmp.SnmpConfig;
import org.opennms.netmgt.sampler.config.snmp.SnmpAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrap an {@link org.opennms.netmgt.api.sample.Agent} instance in a {@link org.opennms.netmgt.sampler.config.snmp.SnmpAgent} instance.
 */
public class JmxAgentProcessor implements Processor {
    public static final Logger LOG = LoggerFactory.getLogger(JmxAgentProcessor.class);

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
            
            LOG.debug("SNMP configuration for address {}: {}", agent.getInetAddress(), def);

            snmpAgent.setCommunity(def.getReadCommunity());
            snmpAgent.setRetries(def.getRetry());
            snmpAgent.setTimeout(def.getTimeout());
            snmpAgent.setVersion(def.getVersion());
        } else {
            LOG.debug("SNMP config factory is not set!");
        }

        exchange.getIn().setBody(snmpAgent);
    }
}