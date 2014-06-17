package org.opennms.minion.controller.core.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.karaf.admin.AdminService;
import org.apache.karaf.admin.Instance;
import org.apache.karaf.admin.InstanceSettings;

final class MockAdminService implements AdminService {
    final Map<String,Instance> m_instances = new HashMap<String,Instance>();

    @Override
    public Instance createInstance(String name, InstanceSettings settings) throws Exception {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public void renameInstance(String name, String newName) throws Exception {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public void refreshInstance() throws Exception {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public Instance cloneInstance(String name, String cloneName, InstanceSettings settings) throws Exception {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public Instance[] getInstances() {
        return m_instances.values().toArray(new Instance[0]);
    }

    @Override
    public Instance getInstance(final String name) {
        return m_instances.get(name);
    }

    public void addInstance(final Instance instance) {
        m_instances.put(instance.getName(), instance);
    }
    
}