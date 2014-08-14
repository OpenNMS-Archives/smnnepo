package org.opennms.netmgt.sampler.jmx.internal;

import java.util.Map;

import org.opennms.netmgt.api.sample.AbsoluteValue;
import org.opennms.netmgt.api.sample.CollectionRequest;
import org.opennms.netmgt.api.sample.CounterValue;
import org.opennms.netmgt.api.sample.DeriveValue;
import org.opennms.netmgt.api.sample.GaugeValue;
import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.api.sample.MetricType;
import org.opennms.netmgt.api.sample.Resource;
import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.api.sample.SampleValue;
import org.opennms.netmgt.collection.api.ServiceParameters;
import org.opennms.netmgt.config.collectd.jmx.Attrib;
import org.opennms.netmgt.config.collectd.jmx.Mbean;
import org.opennms.netmgt.jmx.JmxSampleProcessor;
import org.opennms.netmgt.jmx.JmxUtils;
import org.opennms.netmgt.jmx.samples.JmxAttributeSample;
import org.opennms.netmgt.jmx.samples.JmxCompositeSample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmxCollectionRequest implements CollectionRequest<JmxAgent> {

    private static final Logger LOG = LoggerFactory.getLogger(JmxCollectionRequest.class);

    private final JmxAgent agent;

    public JmxCollectionRequest(JmxAgent agent) {
        this.agent = agent;
    }

    @Override
    public JmxAgent getAgent() {
        return agent;
    }

    @Override
    public String getProtocol() {
        return "JMX";
    }

    public JmxSampleProcessor getSampleProcessor(final SampleSet sampleSet) {
        final String serviceName = agent.getServiceName();
        final String friendlyName = agent.getParameter(ServiceParameters.ParameterName.FRIENDLY_NAME.toString());
        final String collectionDirectoryName = JmxUtils.getCollectionDirectory(agent.getParameters(), friendlyName, serviceName);
        final Resource resource = new Resource(agent, getProtocol(), collectionDirectoryName, serviceName);

        return new JmxSampleProcessor() {

            @Override
            public void process(JmxAttributeSample attributeSample) {
                LOG.info("Processing a {} sample.", attributeSample.getClass());
                sampleSet.addMeasurement(resource, getMetric(attributeSample), getSampleValue(attributeSample));
            }

            @Override
            public void process(JmxCompositeSample compositeSample) {
                LOG.info("Processing a {} sample.", compositeSample.getClass(), compositeSample);
                sampleSet.addMeasurement(resource, getMetric(compositeSample), getSampleValue(compositeSample));
            }

            private SampleValue getSampleValue(JmxCompositeSample sample) {
                return getSampleValue(sample.getCompositeMember().toAttrib(), sample.getCollectedValueAsString());
            }

            private SampleValue getSampleValue(JmxAttributeSample sample) {
                return getSampleValue(sample.getAttrib(), sample.getCollectedValueAsString());
            }

            private SampleValue getSampleValue(Attrib attrib, String value) {
                MetricType metricType = getMetricType(attrib);
                switch(metricType) {
                    case ABSOLUTE:
                        return new AbsoluteValue(Long.valueOf(value));
                    case COUNTER:
                        return new CounterValue(Long.valueOf(value));
                    case DERIVE:
                        return new DeriveValue(Long.valueOf(value));
                    case GAUGE:
                        return new GaugeValue(Double.valueOf(value));
                    default:
                        throw new IllegalArgumentException("No MetricType conversion found.");
                }
            }

            private Metric getMetric(JmxAttributeSample sample) {
                return getMetric(agent.getParameters(), sample.getMbean(), sample.getAttrib());
            }

            // TODO mvr convert to CompositeSample
            // TODO mvr CompositeSample should not inherit AttributeSample (getAttrib()) is not inheritable
            private Metric getMetric(JmxCompositeSample sample) {
                final Attrib attrib = sample.getCompositeMember().toAttrib();
                return getMetric(agent.getParameters(), sample.getMbean(), attrib);
            }

            private Metric getMetric(Map<String, String> parameters, Mbean mbean, Attrib attrib) {
                // Trim it (this is because rrd and collection is not separated correctly)
                // this needs to go away in the future
                final String sampleAlias = JmxUtils.trimAttributeName(attrib.getAlias());
                final MetricType metricType = getMetricType(attrib);
                final String groupName = JmxUtils.getGroupName(parameters, mbean);
                final Metric metric = new Metric(sampleAlias, metricType, groupName);
                return metric;
            }

            /**
             * Converts the Attrib type to a MetricType.
             * The name should match, but may be the case is different.
             * */
            private MetricType getMetricType(Attrib attrib) {
                final String attribType = attrib.getType();
                for (MetricType eachMetricType : MetricType.values()) {
                    if (eachMetricType.name().equalsIgnoreCase(attribType)) {
                        return eachMetricType;
                    }
                }
                throw new IllegalArgumentException("No MetricType for type " + attribType + " found.");
            }
        };
    }
}
