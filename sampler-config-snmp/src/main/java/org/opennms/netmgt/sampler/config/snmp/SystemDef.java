package org.opennms.netmgt.sampler.config.snmp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.opennms.netmgt.config.api.collection.IGroup;
import org.opennms.netmgt.config.api.collection.ISystemDef;
import org.opennms.netmgt.config.api.collection.ITable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
@XmlAccessorType(XmlAccessType.NONE)
public class SystemDef implements ISystemDef {
    private static final Logger LOG = LoggerFactory.getLogger(SystemDef.class);

    private static final Table[] EMPTY_TABLE_ARRAY = new Table[0];
    private static final Group[] EMPTY_GROUP_ARRAY = new Group[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    @XmlAttribute(name="name")
    private String m_name;

    @XmlElement(name="sysoidMask")
    private String m_sysoidMask;

    @XmlElement(name="sysoid")
    private String m_sysoid;

    @XmlElementWrapper(name="collect")
    @XmlElement(name="include")
    private List<String> m_includes = new ArrayList<String>();

    @XmlTransient
    private List<Table> m_tables = new ArrayList<Table>();

    @XmlTransient
    private List<Group> m_groups = new ArrayList<Group>();

    @Override
    public String getName() {
        return m_name;
    }

    public void setName(String name) {
        m_name = name;
    }

    @Override
    public String getSysoidMask() {
        return m_sysoidMask;
    }

    public void setSysoidMask(String sysoidMask) {
        m_sysoidMask = sysoidMask;
    }

    @Override
    public String getSysoid() {
        return m_sysoid;
    }

    public void setSysoid(String sysoid) {
        m_sysoid = sysoid;
    }

    @Override
    public String[] getIncludes() {
        return m_includes == null? EMPTY_STRING_ARRAY : m_includes.toArray(EMPTY_STRING_ARRAY);
    }

    public void setIncludes(final String[] includes) {
        if (includes == null) {
            m_includes.clear();
        } else {
            m_includes = Arrays.asList(includes);
        }
    }

    public ITable[] getTables() {
        return m_tables == null? EMPTY_TABLE_ARRAY : m_tables.toArray(EMPTY_TABLE_ARRAY);
    }

    public void setTables(final ITable[] tables) {
        if (tables == null) {
            m_tables.clear();
        } else {
            m_tables = Arrays.asList(Table.asTables(tables));
        }
    }

    public IGroup[] getGroups() {
        return m_groups == null? EMPTY_GROUP_ARRAY : m_groups.toArray(EMPTY_GROUP_ARRAY);
    }

    public void setGroups(final IGroup[] groups) {
        if (groups == null) {
            m_groups.clear();
        } else {
            m_groups = Arrays.asList(Group.asGroups(groups));
        }
    }

    public void initialize(DataCollectionInitializationCache cache) {
        LOG.debug("{} initializing", m_name);
        for (final String include : m_includes) {
            if (cache.hasTable(include)) {
                final Table table = Table.asTable(cache.getTable(include));
                addTable(table);
            } else if (cache.hasGroup(include)) {
                addGroup(Group.asGroup(cache.getGroup(include)));
            } else {
                throw new IllegalArgumentException("Unable to locate include " + include + " for systemDef " + getName());
            }
        }
        LOG.debug("{} finished initializing", m_name);
    }

    protected void addTable(final Table table) {
        for (final Table t : m_tables) {
            if (t.getName().equals(table.getName())) {
                LOG.trace("Table {} already added.", table.getName());
                return;
            }
        }
        m_tables.add(table);
    }

    protected void addGroup(final Group group) {
        for (final Group g : m_groups) {
            if (g.getName().equals(group.getName())) {
                LOG.trace("Group {} already added.", group.getName());
                return;
            }
        }
        m_groups.add(group);
    }
    public boolean matches(final SnmpAgent agent) {
        final String systemObjId = agent.getSysObjectId();

        if (m_sysoid == null && m_sysoidMask == null) {
            LOG.debug("SystemDef {} has no oid *or* mask, always matches!", m_name);
            return true;
        } else if (systemObjId == null) {
            LOG.debug("Agent {} has no sysObjectId!", agent);
            return true;
        } else if (m_sysoid != null && systemObjId.equals(m_sysoid)) {
            LOG.debug("Agent matches sysoid {}: {}", m_sysoid, agent);
            return true;
        } else if (m_sysoidMask != null && systemObjId.startsWith(m_sysoidMask)) {
            LOG.debug("Agent matches sysoidMask {}: {}", m_sysoidMask, agent);
            return true;
        } else {
            LOG.debug("Agent does not match sysoid={}, sysoidMask={}: {}", m_sysoid, m_sysoidMask, agent);
            return false;
        }
    }

    public void fillRequest(final SnmpCollectionRequest request) {
        assertInitialized();

        if (!matches(request.getAgent())) {
            return;
        }

        for(final Table table : m_tables) {
            table.fillRequest(request);
        }

        for(final Group group : m_groups) {
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

    public static SystemDef asSystemDef(final ISystemDef systemDef) {
        if (systemDef == null) return null;
        
        final SystemDef newDef = new SystemDef();
        newDef.setName(systemDef.getName());
        newDef.setSysoidMask(systemDef.getSysoidMask());
        newDef.setSysoid(systemDef.getSysoid());
        newDef.setIncludes(systemDef.getIncludes());
        newDef.setTables(systemDef.getTables());

        return newDef;
    }

    public static SystemDef[] asSystemDefs(final ISystemDef[] systemDefs) {
        if (systemDefs == null) return null;
        
        final SystemDef[] newDefs = new SystemDef[systemDefs.length];
        for (int i=0; i < systemDefs.length; i++) {
            newDefs[i] = SystemDef.asSystemDef(systemDefs[i]);
        }
        
        return newDefs;
    }

}
