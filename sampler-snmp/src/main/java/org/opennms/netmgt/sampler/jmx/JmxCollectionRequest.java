package org.opennms.netmgt.sampler.jmx;

import org.opennms.netmgt.api.sample.CollectionRequest;
import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.snmp.AggregateTracker;
import org.opennms.netmgt.snmp.Collectable;
import org.opennms.netmgt.snmp.CollectionTracker;

import java.util.ArrayList;
import java.util.List;

public class JmxCollectionRequest implements CollectionRequest<JmxAgent> {

    private JmxAgent agent;

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

    public CollectionTracker getCollectionTracker(SampleSet sampleSet) {
        List<Collectable> trackers = new ArrayList<Collectable>();
        return new AggregateTracker(trackers);
    }
}
