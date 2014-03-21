package org.opennms.netmgt.sampler.config.snmp;

import java.util.ArrayList;
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

@XmlRootElement(name="snmp-collection")
@XmlAccessorType(XmlAccessType.NONE)
public class SnmpCollection implements ISnmpCollection {

    @XmlAttribute(name="name")
    private String m_name="default";

    @XmlElement(name="include-collection")
    private GroupReference[] m_includedGroups;

    @XmlElement(name="datacollection-group")
    private DataCollectionGroup[] m_dataCollectionGroups;

    @XmlElement(name="rrd")
    private Rrd m_rrd;

    @Override
    public String getName() {
        return m_name;
    }

    @Override
    public IGroupReference[] getIncludedGroups() {
        return m_includedGroups;
    }

    @Override
    public IDataCollectionGroup[] getDataCollectionGroups() {
        return m_dataCollectionGroups;
    }

    public void initialize(Map<String, ? extends IDataCollectionGroup> availableGroups) {
        if (m_includedGroups != null) {
            final List<IDataCollectionGroup> groupList = new ArrayList<IDataCollectionGroup>(m_includedGroups.length);

            for(final GroupReference ref : m_includedGroups) {
                final String groupName = ref.getDataCollectionGroup();
                final IDataCollectionGroup group = availableGroups.get(groupName);
                if (group == null) {
                    throw new IllegalArgumentException("Unable to locate datacollection-group " + groupName);
                }
                groupList.add(group);
            }

            m_dataCollectionGroups = groupList.toArray(new DataCollectionGroup[groupList.size()]);
        }
    }

    public void fillRequest(SnmpCollectionRequest request) {
        assertInitialized();
        for (DataCollectionGroup group : m_dataCollectionGroups) {
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
