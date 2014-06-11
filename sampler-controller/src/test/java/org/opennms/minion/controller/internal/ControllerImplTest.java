package org.opennms.minion.controller.internal;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ControllerImplTest {
    @Test
    public void testInit() throws Exception {
        final MockConfigurationAdmin configurationAdmin = new MockConfigurationAdmin();
        final ControllerImpl controller = new ControllerImpl();
        controller.setConfigurationAdmin(configurationAdmin);
        controller.init();
        
        assertNotNull(controller.getId());
    }

    private static final class MockConfigurationAdmin implements ConfigurationAdmin {
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
    }

    private static final class MockConfiguration implements Configuration {
        private final String m_pid;
        private Hashtable<String,Object> m_properties;

        private MockConfiguration(final String pid) {
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
}
