package org.opennms.netmgt.sampler.config.snmp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;


/**
 *  <systemDef name="Enterprise">
 *    <sysoidMask>.1.3.6.1.4.1.</sysoidMask>
 *    <collect>
 *      <include>mib2-host-resources-storage</include>
 *      <include>mib2-coffee-rfc2325</include>
 *    </collect>
 *  </systemDef>
 *   
 * @author brozow
 *
 */
@XmlRootElement(name="datacollection-group")
@XmlAccessorType(XmlAccessType.FIELD)
public class SystemDef {

	@XmlAttribute(name="name")
	private String m_name;
	
	@XmlElement(name="sysoidMask")
	private String m_sysoidMask;
	
	@XmlElement(name="sysoid")
	private String m_sysoid;
	
	@XmlElementWrapper(name="collect")
	@XmlElement(name="include")
	private String[] m_includes;
	
	@XmlTransient
	private Table[] m_tables;
	
	@XmlTransient
	private Group[] m_groups;

	public String getName() {
		return m_name;
	}

	public void setName(String name) {
		m_name = name;
	}

	public String getSysoidMask() {
		return m_sysoidMask;
	}

	public void setSysoidMask(String sysoidMask) {
		m_sysoidMask = sysoidMask;
	}

	public String getSysoid() {
		return m_sysoid;
	}

	public void setSysoid(String sysoid) {
		m_sysoid = sysoid;
	}

	public String[] getIncludes() {
		return m_includes;
	}

	public void setIncludes(String[] includes) {
		m_includes = includes;
	}
	
	public void initialize(Map<String, Table> tableMap,	Map<String, Group> groupMap) {
		List<Table> tables = new ArrayList<Table>();
		List<Group> groups = new ArrayList<Group>();
		for(String include : m_includes) {
			if (tableMap.containsKey(include)) {
				tables.add(tableMap.get(include));
			} else if (groupMap.containsKey(include)) {
				groups.add(groupMap.get(include));
			} else {
				throw new IllegalArgumentException("Unable to locate include " + include + " for systemDef " + getName());
			}
		}
		
		m_tables = tables.toArray(new Table[tables.size()]);
		m_groups = groups.toArray(new Group[groups.size()]);
		
	}
	
	public boolean matches(SnmpAgent agent) {
		String systemObjId = agent.getSystemObjId();
		
		return (m_sysoid == null && m_sysoidMask == null)
			|| (m_sysoid != null && systemObjId.equals(m_sysoid))
			|| (m_sysoidMask != null && systemObjId.startsWith(m_sysoidMask));
	}

	public void fillRequest(SnmpCollectionRequest request) {
		assertInitialized();
		
		if (!matches(request.getAgent())) return;
		
		for(Table table : m_tables) {
			table.fillRequest(request);
		}
		
		for(Group group : m_groups) {
			group.fillRequest(request);
		}

	}

	private void assertInitialized() {
		if (m_tables == null || m_groups == null) {
			throw new IllegalStateException("systemDef " + getName() + " is not initialized!");
		}
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder("SystemDef[");
		buf.append("name=").append(m_name).append(", ");
		if (m_sysoid != null) {
			buf.append("sysoid=").append(m_sysoid);
		} else if (m_sysoidMask != null) {
			buf.append("sysoidMask=").append(m_sysoidMask);
		} else {
			buf.append("No Match Criteria");
		}
		buf.append("]");
		return buf.toString();
	}

	
	
	
}
