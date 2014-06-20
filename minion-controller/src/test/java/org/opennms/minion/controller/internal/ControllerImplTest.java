package org.opennms.minion.controller.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.Date;

import javax.xml.bind.JAXBContext;

import org.apache.karaf.admin.Instance;
import org.junit.Before;
import org.junit.Test;
import org.opennms.minion.api.MinionController;
import org.opennms.minion.api.MinionStatusMessage;
import org.opennms.minion.controller.internal.MinionControllerImpl;
import org.opennms.minion.controller.internal.MinionStatusMessageImpl;

public class ControllerImplTest {
    private static Date m_testStart = new Date();
    private MockConfigurationAdmin m_configurationAdmin = null;
    private MockAdminService m_adminService = null;
    private MockJmsService m_jmsService = null;
    private MinionControllerImpl m_controller = null;

    @Before
    public void setUp() throws Exception {
        m_configurationAdmin = new MockConfigurationAdmin();
        m_configurationAdmin.setProperty(MinionController.PID, "location", "Test");
        m_configurationAdmin.setProperty(MinionController.PID, "broker", "vm://test?create=true");
        m_adminService = new MockAdminService();
        m_jmsService = new MockJmsService();

        final Instance rootInstance = new MockInstance("root");
        m_adminService.addInstance(rootInstance);

        m_controller = new MinionControllerImpl();
        m_controller.setAdminService(m_adminService);
        m_controller.setConfigurationAdmin(m_configurationAdmin);
        m_controller.setJmsService(m_jmsService);
        m_controller.init();
    }

    @Test
    public void testInit() throws Exception {
        assertNotNull(m_controller.getId());
        assertNotNull(m_controller.getBrokerURI());
        assertEquals(1, m_jmsService.getMessageCount());
        
        final String messageText = m_jmsService.getMessages().get(0);
        assertNotNull(messageText);

        final StringReader reader = new StringReader(messageText);
        final MinionStatusMessageImpl minionStatus = (MinionStatusMessageImpl) (JAXBContext.newInstance(MinionStatusMessageImpl.class).createUnmarshaller()).unmarshal(reader);
        assertNotNull(minionStatus.getId());
        assertEquals(36, minionStatus.getId().length());
        assertEquals("Test", minionStatus.getLocation());
        assertEquals(Instance.STARTED, minionStatus.getStatus());
        assertTrue(minionStatus.getDate().getTime() > m_testStart.getTime());
    }

    @Test
    public void testGetStatus() throws Exception {
        final MinionStatusMessage status = m_controller.getStatus();
        assertNotNull(status);
        assertNotNull(status.getId());
        assertNotNull(status.getLocation());
        assertEquals(Instance.STARTED, status.getStatus());
        assertNotNull(status.getDate());
    }
}
