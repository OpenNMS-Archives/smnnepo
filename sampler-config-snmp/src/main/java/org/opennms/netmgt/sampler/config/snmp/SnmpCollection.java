package org.opennms.netmgt.sampler.config.snmp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.opennms.netmgt.api.sample.Metric;

@XmlRootElement(name="snmp-collection")
public class SnmpCollection {
	
	@XmlAttribute(name="name")
	private String m_name="default";
	
	@XmlAttribute(name="snmpStorageFlag")
	private String m_snmpStorageFlag;
	
	@XmlElement(name="include-collection")
	private GroupReference[] m_includedGroups;
	
	@XmlTransient
	private DataCollectionGroup[] m_dataCollectionGroups;

	public String getName() {
		return m_name;
	}
	
	public String getSnmpStorageFlag() {
		return m_snmpStorageFlag;
	}
	
	public GroupReference[] getIncludedGroups() {
		return m_includedGroups;
	}

	public void initialize(Map<String, DataCollectionGroup> availableGroups) {
		List<DataCollectionGroup> groupList = new ArrayList<DataCollectionGroup>(m_includedGroups.length);
		
		for(GroupReference ref : m_includedGroups) {
			String groupName = ref.getDataCollectionGroup();
			DataCollectionGroup group = availableGroups.get(groupName);
			if (group == null) {
				throw new IllegalArgumentException("Unable to locate datacollection-group " + groupName);
			}
			
			groupList.add(group);

		}
		
		m_dataCollectionGroups = groupList.toArray(new DataCollectionGroup[groupList.size()]);
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
