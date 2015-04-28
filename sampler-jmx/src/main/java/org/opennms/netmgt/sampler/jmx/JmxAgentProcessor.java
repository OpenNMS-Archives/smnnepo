package org.opennms.netmgt.sampler.jmx;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.support.SingletonBeanFactory;
import org.opennms.netmgt.config.collectd.CollectdConfiguration;
import org.opennms.netmgt.config.collectd.Collector;
import org.opennms.netmgt.config.collectd.jmx.JmxCollection;
import org.opennms.netmgt.config.collectd.jmx.JmxDatacollectionConfig;
import org.opennms.netmgt.jmx.ParameterName;
import org.opennms.netmgt.jmx.connection.JmxConnectors;
import org.opennms.netmgt.sampler.jmx.internal.JmxAgent;

public class JmxAgentProcessor implements Processor {

    /**
     * TODO: This should be of type JmxDatacollectionConfig
     *
     * Currently, when the type is specified, the blueprint fails to load with:
     *   Unable to convert value SingletonBeanFactory[ instance=null ] to type
     *   org.opennms.netmgt.api.sample.support.SingletonBeanFactory<org.opennms.netmgt.config.collectd.jmx.JmxDatacollectionConfig>
     */
    private SingletonBeanFactory<?> jmxConfigFactory;

    private SingletonBeanFactory<CollectdConfiguration> collectdConfigFactory;

    public void setJmxConfigFactory(final SingletonBeanFactory<?> factory) {
        jmxConfigFactory = factory;
    }

    public void setCollectdConfigFactory(final SingletonBeanFactory<CollectdConfiguration> factory) {
        collectdConfigFactory = factory;
    }

    @Override
    public void process(final Exchange exchange) throws Exception {
        final Agent agent = exchange.getIn().getBody(Agent.class);
        final JmxAgent jmxAgent = new JmxAgent(agent);

        final String serviceClassName = getServiceClassName(jmxAgent);

        final String collection = Objects.toString(jmxAgent.getParameter(ParameterName.COLLECTION.toString()), "jsr160");
        final String retry = Objects.toString(jmxAgent.getParameter(ParameterName.RETRY.toString()), "3");
        final String connectorName = Objects.toString(getJmxConnectorName(serviceClassName), JmxConnectors.JSR160);
        final JmxDatacollectionConfig jmxDatacollectionConfig = (JmxDatacollectionConfig)jmxConfigFactory.getInstance();
        final JmxCollection jmxCollection = jmxDatacollectionConfig != null ? jmxDatacollectionConfig.getJmxCollection(collection) : null;

        jmxAgent.setParameter(ParameterName.COLLECTION.toString(), collection);
        jmxAgent.setParameter(ParameterName.RETRY.toString(), retry);
        jmxAgent.setConnectorName(connectorName);
        jmxAgent.setCollection(jmxCollection);

        exchange.getIn().setBody(jmxAgent);
    }

    // get the class name for the service from the collector list
    private String getServiceClassName(JmxAgent agent) {
        final CollectdConfiguration collectdConfiguration = collectdConfigFactory.getInstance();
        for (Collector eachCollector : collectdConfiguration.getCollectors()) {
            if (Objects.equals(agent.getServiceName(), eachCollector.getService())) {
                final String className = eachCollector.getClassName();
                return className;
            }
        }
        // not found
        return null;
    }

    /**
     * This is needed for backwards compatibility and maps
     * the collectd-configuration.xml classes to jmx connector names.
     */
    private String getJmxConnectorName(String collectorClass) {
        final Map<String, String> mapping = new HashMap<>();
        mapping.put("org.opennms.netmgt.collectd.Jsr160Collector", JmxConnectors.JSR160);
        mapping.put("org.opennms.netmgt.collectd.JBossCollector", JmxConnectors.JBOSS);
        mapping.put("org.opennms.netmgt.collectd.MX4JCollector", JmxConnectors.MX4J);
        mapping.put("org.opennms.netmgt.collectd.JMXSecureCollector", JmxConnectors.JMX_SECURE);

        final String connectorName = mapping.get(collectorClass);
        return connectorName;
    }
}
