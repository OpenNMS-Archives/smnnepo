package org.opennms.netmgt.sampler.jmx.internal;

import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.api.sample.Timestamp;
import org.opennms.netmgt.collectd.JMXCollector;
import org.opennms.netmgt.collectd.Jsr160Collector;
import org.opennms.netmgt.collection.api.CollectionSet;
import org.opennms.netmgt.sampler.jmx.JmxAgent;
import org.opennms.netmgt.sampler.jmx.JmxCollectionRequest;
import org.opennms.netmgt.sampler.jmx.JmxCollector;
import org.opennms.netmgt.sampler.snmp.CollectionException;
import org.opennms.netmgt.snmp.CollectionTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultJmxCollector implements JmxCollector {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultJmxCollector.class);

    /* (non-Javadoc)
     * @see org.opennms.netmgt.sampler.snmp.SnmpCollector#collect(org.opennms.distributed.configuration.snmp.SnmpCollectionRequest)
     */
    @Override
    public SampleSet collect(final JmxCollectionRequest request) throws Exception {
        final SampleSet sampleSet = new SampleSet(Timestamp.now());
//        final JmxAgentConfig agentConfig = request.getAgentConfig();
        final JmxAgent agent = request.getAgent();
        final CollectionTracker collectionTracker = request.getCollectionTracker(sampleSet);

//        LOG.debug("collect: tracker = {}, agent = {}, agentConfig = {}", collectionTracker, agent, agentConfig);

        try {
            // TODO mvr initialize the right collector
            JMXCollector jmxCollector = new Jsr160Collector();
            //jmxCollector.initialize(null); // we do not need to do this
            jmxCollector.initialize(null, null);
            CollectionSet collectionSet = jmxCollector.collect(null, null, null);

            fill(sampleSet, collectionSet);



            return sampleSet;
        } catch (final Exception e) {
            throw new CollectionException(e);
        }

    }

    private void fill(SampleSet sampleSet, CollectionSet collectionSet) {
        // TODO mvr apply data from callectionset to sampleset
    }

}
