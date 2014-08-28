package org.opennms.minion.controller.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.karaf.admin.AdminService;
import org.apache.karaf.admin.Instance;
import org.junit.Before;
import org.junit.Test;
import org.opennms.minion.api.MinionController;
import org.opennms.minion.api.MinionException;
import org.opennms.minion.api.MinionMessage;
import org.opennms.minion.api.MinionMessageSender;
import org.opennms.minion.api.MinionStatusMessage;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class MinionControllerImplTest {
    public class MockMessageSender implements MinionMessageSender {
        private final List<MinionMessage> m_messages = new ArrayList<MinionMessage>();

        @Override
        public void sendMessage(final MinionMessage message) throws MinionException {
            m_messages.add(message);
        }

        public void clear() {
            m_messages.clear();
        }

        public List<MinionMessage> getMessages() {
            return m_messages;
        }

    }

    private ConfigurationAdmin m_configurationAdmin = null;
    private AdminService m_adminService = null;
    private MinionControllerImpl m_controller = null;
    private MockMessageSender m_sender;

    @Before
    public void setUp() throws Exception {
        m_configurationAdmin = mock(ConfigurationAdmin.class);

        final Hashtable<String,Object> properties = new Hashtable<String,Object>();
        properties.put("location", "Test");
        properties.put("broker", "vm://test?create=true");

        final Configuration config = mock(Configuration.class);
        when(config.getProperties()).thenReturn(properties);
        when(m_configurationAdmin.getConfiguration(MinionController.PID)).thenReturn(config);

        final Instance rootInstance = mock(Instance.class);
        when(rootInstance.isRoot()).thenReturn(true);
        when(rootInstance.getName()).thenReturn("root");
        when(rootInstance.getState()).thenReturn(Instance.STARTED);

        m_adminService = mock(AdminService.class);
        final Instance[] instances = new Instance[] {rootInstance};
        when(m_adminService.getInstances()).thenReturn(instances);

        m_sender = new MockMessageSender();

        m_controller = new MinionControllerImpl();
        m_controller.setAdminService(m_adminService);
        m_controller.setConfigurationAdmin(m_configurationAdmin);
        m_controller.setMessageSender(m_sender);
        m_controller.setCamelContext(mock(CamelContext.class));
        m_controller.setDominionBrokerUri("vm://localhost");
        m_controller.setSendQueueName("initialization");
        m_controller.setLocation("MyLocation");
        m_controller.setOpennmsRestRoot("http://localhost:8980");

        m_controller.start();
    }

    @Test
    public void testInit() throws Exception {
        assertNotNull(m_controller.getId());
        assertEquals(1, m_sender.getMessages().size());
        assertTrue(m_sender.getMessages().get(0) instanceof MinionStatusMessage);
    }

    @Test
    public void testGetStatus() throws Exception {
        final MinionStatusMessage status = m_controller.createStatusMessage(null);
        assertNotNull(status);
        assertNotNull(status.getId());
        assertNotNull(status.getLocation());
        assertEquals(Instance.STARTED, status.getStatus());
        assertNotNull(status.getDate());
    }
}
