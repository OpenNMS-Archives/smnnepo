package org.opennms.netmgt.api.sample;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Resource implements Comparable<Resource>, Serializable {
	private static final long serialVersionUID = 2L;

	private final Agent m_agent;
	private final String m_name;
	private final String m_type;
	private final Map<String, String> m_attributes = new HashMap<String, String>();;

	public Resource(Agent agent, String name) {
		this(agent, null, name);
	}

	public Resource(Agent agent, String type, String name) {
		m_agent = agent;
		m_name = name;
		m_type = type;
	}

	public String getIdentifier() {
		return String.format("%s|%s|%s", m_agent.getId(), m_type, m_name);
	}

	public String getType() {
		return m_type;
	}

	public String getName() {
		return m_name;
	}

	public Agent getAgent() {
		return m_agent;
	}

	@Override
	public boolean equals(Object other) {
		return getIdentifier().equals(((Resource)other).getIdentifier());
	}

	@Override
	public int hashCode() {
		return getIdentifier().hashCode();
	}

	@Override
	public int compareTo(Resource other) {
		return getIdentifier().compareTo(other.getIdentifier());
	}

	public void setAttribute(String attrName, String attrValue) {
		m_attributes.put(attrName, attrValue);
	}
	
	public String getAttribute(String attrName) {
		return m_attributes.get(attrName);
	}
	
	public Map<String, String> getAttributes() {
		return m_attributes;
	}

	@Override
	public String toString() {
		return getIdentifier();
	}
}
