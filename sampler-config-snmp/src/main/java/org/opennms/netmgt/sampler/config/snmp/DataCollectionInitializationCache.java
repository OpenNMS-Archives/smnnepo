package org.opennms.netmgt.sampler.config.snmp;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataCollectionInitializationCache {
    private static final Logger LOG = LoggerFactory.getLogger(DataCollectionInitializationCache.class);

    private final Map<String,DataCollectionGroup> m_dataCollectionGroups = new LinkedHashMap<String,DataCollectionGroup>();
    private final Map<String, ResourceType> m_types = new LinkedHashMap<String, ResourceType>();
    private final Map<String, Table> m_tables = new LinkedHashMap<String, Table>();
    private final Map<String, Group> m_groups = new LinkedHashMap<String, Group>();

    public void prepare() {
        for (final DataCollectionGroup group : m_dataCollectionGroups.values()) {
            group.gatherSymbols(this);
        }
        for (final DataCollectionGroup group : m_dataCollectionGroups.values()) {
            group.initialize(this);
        }
    }

    public DataCollectionGroup getDataCollectionGroup(final String groupName) throws IllegalArgumentException {
        if (!m_dataCollectionGroups.containsKey(groupName)) {
            throw new IllegalArgumentException("Unable to locate DataCollectionGroup " + groupName + " in the data collection cache!");
        }
        return m_dataCollectionGroups.get(groupName);
    }

    public void addDataCollectionGroup(final DataCollectionGroup group) {
        final String name = group.getName();
        if (m_dataCollectionGroups.containsKey(name)) {
            LOG.trace("Duplicate DataCollectionGroup: {} - ignoring.", name);
            return;
        }

        LOG.trace("Adding group {} to the datacollection cache.", name);
        m_dataCollectionGroups.put(name, group);
    }

    public void addType(final ResourceType type) {
        final String name = type.getTypeName();
        if (m_types.containsKey(name)) {
            LOG.trace("Duplicate ResourceType: {} - ignoring.", name);
            return;
        }

        LOG.trace("Adding type {} to the datacollection cache.", name);
        m_types.put(name, type);
    }

    public void addTable(final Table table) {
        final String name = table.getName();
        if (m_tables.containsKey(name)) {
            LOG.trace("Duplicate Table: {} - ignoring.", name);
            return;
        }

        LOG.trace("Adding table {} to the datacollection cache.", name);
        m_tables.put(name, table);
    }

    public void addGroup(final Group group) {
        final String name = group.getName();
        if (m_groups.containsKey(name)) {
            LOG.trace("Duplicate Group: {} - ignoring", name);
            return;
        }

        LOG.trace("Adding group {} to the datacollection cache.", name);
        m_groups.put(name, group);
    }

    public Collection<DataCollectionGroup> getDataCollectionGroups() {
        return m_dataCollectionGroups.values();
    }

    public Collection<ResourceType> getTypes() {
        return m_types.values();
    }
    
    public Collection<Table> getTables() {
        return m_tables.values();
    }
    
    public Collection<Group> getGroups() {
        return m_groups.values();
    }

    public boolean hasType(final String name) {
        return m_types.containsKey(name);
    }

    public ResourceType getType(final String name) {
        if (!m_types.containsKey(name)) {
            throw new IllegalArgumentException("Unable to locate resourceType " + name + "!");
        }
        return m_types.get(name);
    }

    public boolean hasTable(final String name) {
        return m_tables.containsKey(name);
    }
    
    public Table getTable(final String name) {
        if (!m_tables.containsKey(name)) {
            throw new IllegalArgumentException("Unable to locate Table " + name + "!");
        }
        return m_tables.get(name);
    }

    public boolean hasGroup(final String name) {
        return m_groups.containsKey(name);
    }
    
    public Group getGroup(final String name) {
        if (!m_groups.containsKey(name)) {
            throw new IllegalArgumentException("Unable to locate Group " + name + "!");
        }
        return m_groups.get(name);
    }

    @Override
    public String toString() {
        return "DataCollectionInitializationCache[dataCollectionGroups=" + m_dataCollectionGroups.keySet() + ",types=" + m_types.keySet() + ",tables=" + m_tables.keySet() + ",groups=" + m_groups.keySet() + "]";
    }
}
