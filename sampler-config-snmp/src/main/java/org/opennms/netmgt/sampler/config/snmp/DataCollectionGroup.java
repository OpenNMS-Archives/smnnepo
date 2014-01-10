package org.opennms.netmgt.sampler.config.snmp;

import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.netmgt.api.sample.Metric;

@XmlRootElement(name="datacollection-group")
@XmlAccessorType(XmlAccessType.FIELD)
public class DataCollectionGroup {

	@XmlAttribute(name="name")
	String m_name;
	
	@XmlElement(name="resourceType")
	ResourceType[] m_resourceTypes = new ResourceType[0];
	
	@XmlElement(name="table")
	Table[] m_tables = new Table[0];
	
	@XmlElement(name="group")
	Group[] m_groups = new Group[0];
	
	@XmlElement(name="systemDef")
	SystemDef[] m_systemDefs = new SystemDef[0];

	public Group[] getGroups() {
		return m_groups;
	}
	
	public Table[] getTables() {
		return m_tables;
	}

	public SystemDef[] getSystemDefs() {
		return m_systemDefs;
	}

	public ResourceType[] getResourceTypes() {
		return m_resourceTypes;
	}

	public String getName() {
		return m_name;
	}

	public void initialize(Map<String, ResourceType> typeMap, Map<String, Table> tableMap, Map<String, Group> groupMap) {
		
		for(Table table : m_tables) {
			String typeName = table.getInstance();
			ResourceType type = typeMap.get(typeName);
			if (type == null) {
				throw new IllegalArgumentException("Unable to locate resourceType " + typeName + " for table " + table.getName());
			}
			table.initialize(type);
		}
		
		for (Group group : m_groups) {
		    group.initialize();
		}

		for(SystemDef systemDef : m_systemDefs) {
			systemDef.initialize(tableMap, groupMap);
		}
	}

	public void fillRequest(SnmpCollectionRequest request) {
		for(SystemDef systemDef : m_systemDefs) {
			systemDef.fillRequest(request);
		}
	}

	public void gatherSymbols(Map<String, ResourceType> typeMap, Map<String, Table> tableMap, Map<String, Group> groupMap) {
		for(ResourceType resourceType : m_resourceTypes) {
			typeMap.put(resourceType.getTypeName(), resourceType);
		}
		for(Table table : m_tables) {
			tableMap.put(table.getName(), table);
		}
		for(Group group : m_groups) {
			groupMap.put(group.getName(), group);
		}
	}

	public Set<Metric> getMetricsForGroup(String groupName) {
		for(Table table : m_tables) {
			if (groupName.equals(table.getName())) {
				return table.getMetrics();
			}
		}
		
		for(Group group : m_groups) {
			if (groupName.equals(group.getName())) {
				return group.getMetrics();
			}
		}
		
		return null;
	}

	public Metric getMetric(String metricName) {
		for(Table table : m_tables) {
			Metric metric = table.getMetric(metricName);
			if (metric != null) { return metric; }
		}
		
		for(Group group : m_groups) {
			Metric metric = group.getMetric(metricName);
			if (metric != null) { return metric; }
		}
		
		return null;
	}


}
