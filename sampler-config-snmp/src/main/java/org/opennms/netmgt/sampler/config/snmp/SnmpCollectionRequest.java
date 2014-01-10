package org.opennms.netmgt.sampler.config.snmp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opennms.netmgt.api.sample.CollectionRequest;
import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.snmp.AggregateTracker;
import org.opennms.netmgt.snmp.Collectable;
import org.opennms.netmgt.snmp.CollectionTracker;
import org.opennms.netmgt.snmp.SnmpAgentConfig;

public class SnmpCollectionRequest implements CollectionRequest<SnmpAgent> {
	
	private SnmpAgent m_agent;
	
	final private Map<String, ResourceType> m_typeMap = new HashMap<String, ResourceType>();
	final private Map<String, List<Table>> m_tablesByResourceType = new HashMap<String, List<Table>>();
	final private List<Table> m_tables = new ArrayList<Table>();
	final private List<Group> m_groups = new ArrayList<Group>();

	public SnmpCollectionRequest(SnmpAgent agent) {
		m_agent = agent;
	}
	
	public String getProtocol() {
		return "SNMP";
	}

	public SnmpAgent getAgent() {
		return m_agent;
	}
	
	public SnmpAgentConfig getAgentConfig() throws UnknownHostException {
		InetAddress addr = m_agent.getInetAddress();
		SnmpAgentConfig config = new SnmpAgentConfig(addr);
		//config.setPort(9161);
		config.setPort(m_agent.getPort());
		config.setVersionAsString(m_agent.getVersion());
		config.setReadCommunity(m_agent.getCommunity());
		config.setTimeout(m_agent.getTimeout());
		config.setRetries(m_agent.getRetries());
		return config;
	}

	public void addResourceType(ResourceType resourceType) {
		m_typeMap.put(resourceType.getTypeName(), resourceType);
	}

	public void addTable(Table table) {
		List<Table> tables = m_tablesByResourceType.get(table.getInstance());
		if (tables == null) {
			tables = new ArrayList<Table>();
			m_tablesByResourceType.put(table.getInstance(), tables);
		}
		tables.add(table);
		m_tables.add(table);
	}

	public void addGroup(Group group) {
		m_groups.add(group);
	}

	public Collection<ResourceType> getResourceTypes() {
		return m_typeMap.values();
	}
	
	public List<Table> getTables() {
		return m_tables;
	}
	
	public List<Group> getGroups() {
		return m_groups;
	}
	
	public String toString() {
		StringBuilder buf = new StringBuilder("CollectionRequest[");
		buf.append("types=").append(getResourceTypes());
		buf.append(", ").append("tables=").append(getTables());
		buf.append(", ").append("groups=").append(getGroups());
		buf.append("]");
		return buf.toString();
	}

	public CollectionTracker getCollectionTracker(SampleSet sampleSet) {
		List<Collectable> trackers = new ArrayList<Collectable>();
		
		for(Table table : m_tables) {
			trackers.add(table.createCollectionTracker(m_agent, sampleSet));
		}
		
		for (Group group : m_groups) {
		    trackers.add(group.createCollectionTracker(m_agent, sampleSet));
		}

		return new AggregateTracker(trackers);

	}

}
