package org.opennms.netmgt.sampler.config.snmp;

import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.netmgt.api.sample.Metric;

@XmlRootElement(name="datacollection-config")
@XmlAccessorType(XmlAccessType.FIELD)
public class DataCollectionConfig {

	@XmlElement(name="snmp-collection")
	SnmpCollection[] m_snmpCollections;	

	public SnmpCollection[] getSnmpCollections() {
		return m_snmpCollections;
	}

	public void initialize(Map<String, DataCollectionGroup> availableGroups) {
		for (SnmpCollection snmpCollection : m_snmpCollections) {
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
}
