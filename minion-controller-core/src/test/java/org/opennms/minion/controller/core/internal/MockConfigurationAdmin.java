package org.opennms.minion.controller.core.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

final class MockConfigurationAdmin implements ConfigurationAdmin {
    final Map<String,Configuration> m_configurations = new HashMap<String,Configuration>();

    @Override
    public Configuration createFactoryConfiguration(String factoryPid) throws IOException {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public Configuration createFactoryConfiguration(String factoryPid, String location) throws IOException {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public Configuration getConfiguration(String pid, String location) throws IOException {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public Configuration getConfiguration(final String pid) throws IOException {
        if (!m_configurations.containsKey(pid)) {
            m_configurations.put(pid, new MockConfiguration(pid));
        }
        return m_configurations.get(pid);
    }

    @Override
    public Configuration[] listConfigurations(String filter) throws IOException, InvalidSyntaxException {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    public void setProperty(final String pid, final String key, final String value) throws IOException {
        final Configuration configuration = getConfiguration(pid);
        final Dictionary<String,Object> existing = configuration.getProperties();
        final Dictionary<String,Object> updated = new Hashtable<String,Object>();
        if (existing != null) {
            for (final String k : Collections.list(existing.keys())) {
                updated.put(k, existing.get(k));
            }
        }
        updated.put(key, value);
        
        configuration.update(updated);
    }
}