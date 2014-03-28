package org.opennms.netmgt.sampler.config.snmp;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.config.api.collection.IDataCollectionConfig;
import org.opennms.netmgt.config.api.collection.IDataCollectionGroup;
import org.opennms.netmgt.config.api.collection.ISnmpCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlRootElement(name="datacollection-config")
@XmlAccessorType(XmlAccessType.NONE)
public class DataCollectionConfig implements IDataCollectionConfig {
    private static final Logger LOG = LoggerFactory.getLogger(DataCollectionConfig.class);

    private static final ISnmpCollection[] EMPTY_SNMP_COLLECTION_ARRAY = new ISnmpCollection[0];

    @XmlElement(name="snmp-collection")
    List<SnmpCollection> m_snmpCollections = new ArrayList<SnmpCollection>();

    public ISnmpCollection[] getSnmpCollections() {
        return m_snmpCollections.toArray(EMPTY_SNMP_COLLECTION_ARRAY);
    }
    
    public void initialize() {
        initialize(new DataCollectionInitializationCache());
    }

    public void initialize(final DataCollectionInitializationCache cache) {
        LOG.debug("datacollection config initializing with cache: {}", cache);

        for (final SnmpCollection snmpCollection : m_snmpCollections) {
            final IDataCollectionGroup[] dataCollectionGroups = snmpCollection.getDataCollectionGroups();
            if (dataCollectionGroups != null) {
                for (final IDataCollectionGroup group : dataCollectionGroups) {
                    final DataCollectionGroup collectionGroup = DataCollectionGroup.asCollectionGroup(group);
                    cache.addDataCollectionGroup(collectionGroup);
                }
            }
        }

        LOG.debug("preparing datacollection cache");
        cache.prepare();

        LOG.debug("initializing {} collections", m_snmpCollections == null? 0 : m_snmpCollections.size());
        for (final SnmpCollection snmpCollection : m_snmpCollections) {
            snmpCollection.initialize(cache);
        }
        LOG.debug("datacollection config finished initializing");
    }

    public void fillRequest(final SnmpCollectionRequest request) {
        for (final SnmpCollection snmpCollection : m_snmpCollections) {
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
        return "DataCollectionConfig [snmpCollections=" + m_snmpCollections + "]";
    }
}
