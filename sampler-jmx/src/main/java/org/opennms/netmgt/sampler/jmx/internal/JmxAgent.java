package org.opennms.netmgt.sampler.jmx.internal;

import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.config.collectd.jmx.JmxCollection;
import org.opennms.netmgt.jmx.JmxCollectorConfig;
import org.opennms.netmgt.jmx.ParameterName;
import org.opennms.netmgt.snmp.InetAddrUtils;

public class JmxAgent extends Agent {

    private static final long serialVersionUID = 1L;

    private String connectorName;

    private JmxCollection collection;

    public JmxAgent(Agent agent) {
        super(agent);
    }

    public void setConnectorName(String connectorName) {
        this.connectorName = connectorName;
    }

    public JmxCollectorConfig getCollectorConfig() {
        final JmxCollectorConfig config = new JmxCollectorConfig();
        config.setAgentAddress(InetAddrUtils.str(getInetAddress()));
        config.setRetries(Integer.valueOf(getParameter(ParameterName.RETRY.toString())));
        config.setServiceProperties(getParameters());
        config.setConnectionName(connectorName);
        config.setJmxCollection(collection);
        return config;
    }

    public void setCollection(JmxCollection collection) {
        this.collection = collection;
    }
}
