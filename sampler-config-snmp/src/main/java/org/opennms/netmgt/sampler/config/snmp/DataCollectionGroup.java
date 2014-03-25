package org.opennms.netmgt.sampler.config.snmp;

import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.config.api.collection.IDataCollectionGroup;
import org.opennms.netmgt.config.api.collection.IGroup;
import org.opennms.netmgt.config.api.collection.IResourceType;
import org.opennms.netmgt.config.api.collection.ISystemDef;
import org.opennms.netmgt.config.api.collection.ITable;

@XmlRootElement(name="datacollection-group")
@XmlAccessorType(XmlAccessType.NONE)
public class DataCollectionGroup implements IDataCollectionGroup {

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

    @Override
    public IGroup[] getGroups() {
        return (IGroup[]) m_groups;
    }

    @Override
    public ITable[] getTables() {
        return (ITable[]) m_tables;
    }

    @Override
    public ISystemDef[] getSystemDefs() {
        return (ISystemDef[]) m_systemDefs;
    }

    @Override
    public IResourceType[] getResourceTypes() {
        return m_resourceTypes;
    }

    @Override
    public String getName() {
        return m_name;
    }

    public void initialize(final Map<String, ResourceType> typeMap, final Map<String, Table> tableMap, final Map<String, Group> groupMap) {
        for(Table table : m_tables) {
            final String typeName = table.getInstance();
            final ResourceType type = typeMap.get(typeName);
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

    public void gatherSymbols(final Map<String, ResourceType> typeMap, final Map<String, Table> tableMap, final Map<String, Group> groupMap) {
        for(final ResourceType resourceType : m_resourceTypes) {
            typeMap.put(resourceType.getTypeName(), resourceType);
        }
        for(final Table table : m_tables) {
            tableMap.put(table.getName(), table);
        }
        for(final Group group : m_groups) {
            groupMap.put(group.getName(), group);
        }
    }

    public Set<Metric> getMetricsForGroup(String groupName) {
        for(final Table table : m_tables) {
            if (groupName.equals(table.getName())) {
                return table.getMetrics();
            }
        }

        for(final Group group : m_groups) {
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

    public static DataCollectionGroup asCollectionGroup(final IDataCollectionGroup group) {
        if (group == null) return null;

        if (group instanceof DataCollectionGroup) {
            return (DataCollectionGroup)group;
        } else {
            final DataCollectionGroup dcg = new DataCollectionGroup();
            dcg.m_name = group.getName();
            dcg.m_resourceTypes = ResourceType.asResourceTypes(group.getResourceTypes());
            dcg.m_tables = Table.asTables(group.getTables());
            dcg.m_groups = Group.asGroups(group.getGroups());
            dcg.m_systemDefs = SystemDef.asSystemDefs(group.getSystemDefs());
            return dcg;
        }
    }

    public static DataCollectionGroup[] asCollectionGroups(final IDataCollectionGroup[] groups) {
        if (groups == null) return null;
        
        final DataCollectionGroup[] newGroups = new DataCollectionGroup[groups.length];
        for (int i=0; i < groups.length; i++) {
            newGroups[i] = DataCollectionGroup.asCollectionGroup(groups[i]);
        }
        return newGroups;
    }

}
