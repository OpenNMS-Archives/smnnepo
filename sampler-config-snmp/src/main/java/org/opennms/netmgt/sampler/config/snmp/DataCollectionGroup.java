package org.opennms.netmgt.sampler.config.snmp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlRootElement(name="datacollection-group")
@XmlAccessorType(XmlAccessType.NONE)
public class DataCollectionGroup implements IDataCollectionGroup {
    private static final Logger LOG = LoggerFactory.getLogger(DataCollectionGroup.class);

    private static final IGroup[] EMPTY_GROUP_ARRAY = new IGroup[0];
    private static final ITable[] EMPTY_TABLE_ARRAY = new ITable[0];
    private static final IResourceType[] EMPTY_RESOURCE_TYPE_ARRAY = new IResourceType[0];
    private static final ISystemDef[] EMPTY_SYSTEM_DEF_ARRAY = new ISystemDef[0];

    @XmlAttribute(name="name")
    String m_name;

    @XmlElement(name="resourceType")
    List<ResourceType> m_resourceTypes = new ArrayList<ResourceType>();

    @XmlElement(name="table")
    List<Table> m_tables = new ArrayList<Table>();

    @XmlElement(name="group")
    List<Group> m_groups = new ArrayList<Group>();

    @XmlElement(name="systemDef")
    List<SystemDef> m_systemDefs = new ArrayList<SystemDef>();

    @Override
    public IGroup[] getGroups() {
        return m_groups.toArray(EMPTY_GROUP_ARRAY);
    }

    @Override
    public ITable[] getTables() {
        return m_tables.toArray(EMPTY_TABLE_ARRAY);
    }

    @Override
    public ISystemDef[] getSystemDefs() {
        return m_systemDefs.toArray(EMPTY_SYSTEM_DEF_ARRAY);
    }

    @Override
    public IResourceType[] getResourceTypes() {
        return m_resourceTypes.toArray(EMPTY_RESOURCE_TYPE_ARRAY);
    }

    @Override
    public String getName() {
        return m_name;
    }

    public void initialize(final Map<String, ResourceType> typeMap, final Map<String, Table> tableMap, final Map<String, Group> groupMap) {
        LOG.debug("{} initializing ({} type maps, {} table maps, {} group maps)", m_name, typeMap == null? 0 : typeMap.size(), tableMap == null? 0 : tableMap.size(), groupMap == null? 0 : groupMap.size());

        for(final Table table : m_tables) {
            final String typeName = table.getInstance();
            final ResourceType type = typeMap.get(typeName);
            if (type == null) {
                throw new IllegalArgumentException("Unable to locate resourceType " + typeName + " for table " + table.getName());
            }
            table.initialize(type);
        }

        for (final Group group : m_groups) {
            group.initialize();
        }

        for(final SystemDef systemDef : m_systemDefs) {
            systemDef.initialize(tableMap, groupMap);
        }
        
        LOG.debug("{} finished initializing", m_name);
    }

    public void fillRequest(final SnmpCollectionRequest request) {
        for(final SystemDef systemDef : m_systemDefs) {
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

    public Set<Metric> getMetricsForGroup(final String groupName) {
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

        LOG.trace("No metrics found for group {}", groupName);
        return null;
    }

    public Metric getMetric(final String metricName) {
        for(final Table table : m_tables) {
            final Metric metric = table.getMetric(metricName);
            if (metric != null) { return metric; }
        }

        for(final Group group : m_groups) {
            final Metric metric = group.getMetric(metricName);
            if (metric != null) { return metric; }
        }

        LOG.trace("No metric found: {}", metricName);
        return null;
    }

    public static DataCollectionGroup asCollectionGroup(final IDataCollectionGroup group) {
        if (group == null) return null;

        if (group instanceof DataCollectionGroup) {
            return (DataCollectionGroup)group;
        } else {
            final DataCollectionGroup dcg = new DataCollectionGroup();
            dcg.m_name = group.getName();
            dcg.m_resourceTypes = Arrays.asList(ResourceType.asResourceTypes(group.getResourceTypes()));
            dcg.m_tables = Arrays.asList(Table.asTables(group.getTables()));
            dcg.m_groups = Arrays.asList(Group.asGroups(group.getGroups()));
            dcg.m_systemDefs = Arrays.asList(SystemDef.asSystemDefs(group.getSystemDefs()));
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

    @Override
    public String toString() {
        return "DataCollectionGroup[name=" + m_name + ",resourceTypes=" + m_resourceTypes + ",tables=" + m_tables + ",groups=" + m_groups + ",systemDefs=" + m_systemDefs + "]";
    }
}
