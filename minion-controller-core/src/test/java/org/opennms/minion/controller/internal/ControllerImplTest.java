package org.opennms.minion.controller.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.karaf.admin.Instance;
import org.junit.Before;
import org.junit.Test;
import org.opennms.minion.controller.api.Controller;
import org.opennms.minion.controller.api.IMinionStatus;

public class ControllerImplTest {
    private MockConfigurationAdmin m_configurationAdmin = null;
    private MockAdminService m_adminService = null;
    private ControllerImpl m_controller = null;

    @Before
    public void setUp() throws Exception {
        m_configurationAdmin = new MockConfigurationAdmin();
        m_configurationAdmin.setProperty(Controller.PID, "location", "Test");
        m_adminService = new MockAdminService();

        final Instance rootInstance = new MockInstance("root");
        m_adminService.addInstance(rootInstance);

        m_controller = new ControllerImpl();
        m_controller.setConfigurationAdmin(m_configurationAdmin);
        m_controller.setAdminService(m_adminService);
        m_controller.init();
    }

    @Test
    public void testInit() throws Exception {
        assertNotNull(m_controller.getId());
    }

    @Test
    public void testGetStatus() throws Exception {
        final IMinionStatus status = m_controller.getStatus();
        assertNotNull(status);
        assertNotNull(status.getId());
        assertNotNull(status.getLocation());
        assertEquals(Instance.STARTED, status.getStatus());
        assertNotNull(status.getDate());
    }
}
