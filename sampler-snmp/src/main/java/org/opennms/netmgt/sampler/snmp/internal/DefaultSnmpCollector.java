package org.opennms.netmgt.sampler.snmp.internal;

import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.api.sample.Timestamp;
import org.opennms.netmgt.sampler.config.snmp.SnmpAgent;
import org.opennms.netmgt.sampler.config.snmp.SnmpCollectionRequest;
import org.opennms.netmgt.sampler.snmp.CollectionException;
import org.opennms.netmgt.sampler.snmp.SnmpCollector;
import org.opennms.netmgt.snmp.CollectionTracker;
import org.opennms.netmgt.snmp.SnmpAgentConfig;
import org.opennms.netmgt.snmp.SnmpUtils;
import org.opennms.netmgt.snmp.SnmpWalker;

public class DefaultSnmpCollector implements SnmpCollector {


    /* (non-Javadoc)
     * @see org.opennms.netmgt.sampler.snmp.SnmpCollector#collect(org.opennms.distributed.configuration.snmp.SnmpCollectionRequest)
     */
    @Override
    public SampleSet collect(final SnmpCollectionRequest request) throws Exception {
        try {
            final SampleSet sampleSet = new SampleSet(Timestamp.now());
            final SnmpAgentConfig agentConfig = request.getAgentConfig();
            final SnmpAgent agent = request.getAgent();
            final CollectionTracker collectionTracker = request.getCollectionTracker(sampleSet);
            final SnmpWalker walker = SnmpUtils.createWalker(agentConfig, agent.getId(), collectionTracker);

            walker.start();
            walker.waitFor();

            if (!walker.timedOut() && walker.failed()) {
                throw new CollectionException(walker.getErrorThrowable());
            }

            return sampleSet;
        } catch (final Exception e) {
            if (e instanceof CollectionException) throw e;
            throw new CollectionException(e);
        }

    }

}