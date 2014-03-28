package org.opennms.netmgt.sampler.config.snmp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.config.api.collection.IDataCollectionGroup;
import org.opennms.netmgt.config.api.collection.IGroupReference;
import org.opennms.netmgt.config.api.collection.ISnmpCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlRootElement(name="snmp-collection")
@XmlAccessorType(XmlAccessType.NONE)
public class SnmpCollection implements ISnmpCollection {
    private static final IGroupReference[] EMPTY_GROUP_REFERENCE_ARRAY = new IGroupReference[0];
    private static final IDataCollectionGroup[] EMPTY_DATA_COLLECTION_GROUP_ARRAY = new IDataCollectionGroup[0];

    private static final Logger LOG = LoggerFactory.getLogger(SnmpCollection.class);

    @XmlAttribute(name="name")
    private String m_name="default";

    @XmlElement(name="include-collection")
    private List<GroupReference> m_includedGroups = new ArrayList<GroupReference>();

    @XmlElement(name="datacollection-group")
    private List<DataCollectionGroup> m_dataCollectionGroups = new ArrayList<DataCollectionGroup>();

    @XmlElement(name="rrd")
    private Rrd m_rrd;

    @Override
    public String getName() {
        return m_name;
    }

    @Override
    public IGroupReference[] getIncludedGroups() {
        return m_includedGroups.toArray(EMPTY_GROUP_REFERENCE_ARRAY);
    }

    @Override
    public IDataCollectionGroup[] getDataCollectionGroups() {
        return m_dataCollectionGroups.toArray(EMPTY_DATA_COLLECTION_GROUP_ARRAY);
    }

    public void initialize(final DataCollectionInitializationCache cache) {
        LOG.debug("{} initializing using cache: {}", cache);

        final Map<String,DataCollectionGroup> groups = new LinkedHashMap<String,DataCollectionGroup>();
        for (final DataCollectionGroup group : m_dataCollectionGroups) {
            groups.put(group.getName(), group);
        }

        if (m_includedGroups != null && m_includedGroups.size() > 0) {
            LOG.trace("{} initializing using {} included groups", m_name, m_includedGroups.size());

            for(final GroupReference ref : m_includedGroups) {
                final String groupName = ref.getDataCollectionGroup();
                if (!groups.containsKey(groupName)) {
                    final DataCollectionGroup group = cache.getDataCollectionGroup(groupName);
                    groups.put(groupName, group);
                }
            }
        }

        m_dataCollectionGroups = new ArrayList<DataCollectionGroup>(groups.values());

        LOG.debug("{} initializing all required resources, tables, and groups.", m_name);
        for (final DataCollectionGroup group : m_dataCollectionGroups) {
            group.initialize(cache);
        }

        LOG.debug("{} finished initializing", m_name);
    }

    public void fillRequest(final SnmpCollectionRequest request) {
        assertInitialized();
        for (final DataCollectionGroup group : m_dataCollectionGroups) {
            group.fillRequest(request);
        }
    }

    private void assertInitialized() {
        if (m_dataCollectionGroups == null) {
            throw new IllegalStateException("snmp-collection " + getName() + " is not initialized!");
        }
    }

    public Set<Metric> getMetricsForGroup(String groupName) {
        for(DataCollectionGroup group : m_dataCollectionGroups) {
            Set<Metric> metrics = group.getMetricsForGroup(groupName);
            if (metrics != null) { return metrics; }
        }
        return null;
    }

    public Metric getMetric(String metricName) {
        for(DataCollectionGroup group : m_dataCollectionGroups) {
            Metric metric = group.getMetric(metricName);
            if (metric != null) { return metric; }
        }
        return null;
    }


}
