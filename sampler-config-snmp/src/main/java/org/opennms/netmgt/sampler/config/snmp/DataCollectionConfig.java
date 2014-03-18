package org.opennms.netmgt.sampler.config.snmp;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.config.api.collection.IDataCollectionConfig;
import org.opennms.netmgt.config.api.collection.ISnmpCollection;

@XmlRootElement(name="datacollection-config")
@XmlAccessorType(XmlAccessType.NONE)
public class DataCollectionConfig implements IDataCollectionConfig {

    @XmlElement(name="snmp-collection")
    SnmpCollection[] m_snmpCollections;

    public ISnmpCollection[] getSnmpCollections() {
        return m_snmpCollections;
    }

    public void initialize(final Map<String, DataCollectionGroup> availableGroups) {
        for (final SnmpCollection snmpCollection : m_snmpCollections) {
            snmpCollection.initialize(availableGroups);
        }

    }

    public void fillRequest(SnmpCollectionRequest request) {
        for (SnmpCollection snmpCollection : m_snmpCollections) {
            snmpCollection.fillRequest(request);
        }
    }

    public Set<Metric> getMetricsForGroup(String groupName) {
        for (SnmpCollection snmpCollection : m_snmpCollections) {
            Set<Metric> metrics = snmpCollection.getMetricsForGroup(groupName);
            if (metrics != null) { return metrics; }
        }
        return null;
    }

    public Metric getMetric(String metricName) {
        for (SnmpCollection snmpCollection : m_snmpCollections) {
            Metric metric = snmpCollection.getMetric(metricName);
            if (metric != null) return metric;
        }
        return null;
    }

    @Override
    public String toString() {
        return "DataCollectionConfig [snmpCollections=" + Arrays.toString(m_snmpCollections) + "]";
    }
}
