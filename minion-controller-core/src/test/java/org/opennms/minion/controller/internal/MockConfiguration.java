package org.opennms.minion.controller.internal;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.osgi.service.cm.Configuration;

final class MockConfiguration implements Configuration {
    private final String m_pid;
    private Hashtable<String,Object> m_properties;

    MockConfiguration(final String pid) {
        m_pid = pid;
    }

    @Override
    public String getPid() {
        return m_pid;
    }

    @Override
    public synchronized Dictionary<String, Object> getProperties() {
        if (m_properties != null) {
            final Hashtable<String,Object> properties = new Hashtable<String,Object>();
            properties.putAll(m_properties);
            return properties;
        } else {
            return null;
        }
    }

    @Override
    public synchronized void update(final Dictionary<String, ?> properties) throws IOException {
        if (m_properties == null) {
            m_properties = new Hashtable<String,Object>();
        }

        if (m_properties != properties) {
            m_properties.clear();
            final Enumeration<String> keys = properties.keys();
            while (keys.hasMoreElements()) {
                final String key = keys.nextElement();
                final Object value = properties.get(key);
                m_properties.put(key, value);
            }
        }
    }

    @Override
    public void delete() throws IOException {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public String getFactoryPid() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public void update() throws IOException {
    }

    @Override
    public void setBundleLocation(String location) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public String getBundleLocation() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }
}