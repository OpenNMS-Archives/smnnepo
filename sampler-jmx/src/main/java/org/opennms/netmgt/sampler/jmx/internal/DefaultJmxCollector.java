package org.opennms.netmgt.sampler.jmx.internal;

import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.api.sample.Timestamp;
import org.opennms.netmgt.jmx.JmxCollectorConfig;
import org.opennms.netmgt.jmx.JmxSampleProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultJmxCollector implements JmxCollector {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultJmxCollector.class);

    @Override
    public SampleSet collect(final JmxCollectionRequest request) throws Exception {
        try {
            final SampleSet sampleSet = new SampleSet(Timestamp.now());

            final JmxAgent agent = request.getAgent();
            final JmxCollectorConfig collectorConfig = agent.getCollectorConfig();
            final JmxSampleProcessor sampleProcessor = request.getSampleProcessor(sampleSet);

            LOG.debug("Collect: sampleProcessor =  {}, agent = {}, collectorConfig = {}", sampleProcessor, agent, collectorConfig);

            final org.opennms.netmgt.jmx.JmxCollector collector = new org.opennms.netmgt.jmx.DefaultJmxCollector();
            collector.collect(collectorConfig, sampleProcessor);

            return sampleSet;
        } catch (final Exception e) {
            throw new CollectionException(e);
        }
    }
}
