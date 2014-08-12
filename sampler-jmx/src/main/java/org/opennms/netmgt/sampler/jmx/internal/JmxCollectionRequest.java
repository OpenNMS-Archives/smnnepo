package org.opennms.netmgt.sampler.jmx.internal;

import org.opennms.netmgt.api.sample.CollectionRequest;
import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.jmx.JmxSampleProcessor;
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
        return new JmxSampleProcessor() {
            @Override
            public void process(JmxAttributeSample attributeSample) {
                // TODO mvr set sampleset here
                LOG.info("Processing {}: {}", attributeSample.getClass(), attributeSample);
            }

            @Override
            public void process(JmxCompositeSample compositeSample) {
                // TODO mvr set sampleset here
                LOG.info("Processing {}: {}", compositeSample.getClass(), compositeSample);
            }
        };
    }
}
