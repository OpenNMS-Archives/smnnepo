package org.opennms.minion.controller.internal;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.UUID;

import org.opennms.minion.controller.api.Controller;
import org.opennms.minion.controller.api.ControllerException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerImpl implements Controller {
    private static final Logger LOG = LoggerFactory.getLogger(ControllerImpl.class);
    private static final String PID = "org.opennms.minion.controller";

    private ConfigurationAdmin m_configurationAdmin;

    @Override
    public void init() throws ControllerException {
        LOG.debug("Initializing controller.");
        assert m_configurationAdmin != null : "ConfigurationAdmin is missing!";

        final UUID uuid = getId();
        if (uuid == null) {
            setId(UUID.randomUUID());
        }
        LOG.debug("Controller initialized.  ID is {}.", getId());
    }

    @Override
    public UUID getId() throws ControllerException {
        final Configuration config = getConfiguration();
        final Dictionary<String,Object> properties = config.getProperties();
        if (properties == null) {
            return null;
        }

        final String uuid = (String)properties.get("uuid");
        
        if (uuid == null || uuid.isEmpty()) {
            return null;
        } else {
            return UUID.fromString(uuid);
        }
    }

    private void setId(final UUID uuid) throws ControllerException {
        final Configuration config = getConfiguration();
        final Dictionary<String,Object> properties = config.getProperties() == null? new Hashtable<String,Object>() : config.getProperties();
        properties.put("uuid", uuid.toString());
        try {
            config.update(properties);
        } catch (final IOException e) {
            final ControllerException ce = new ControllerException("Failed to update configuration.", e);
            LOG.error("Unable to update UUID.", ce);
            throw ce;
        }
    }

    private Configuration getConfiguration() throws ControllerException {
        try {
            final Configuration configuration = m_configurationAdmin.getConfiguration(PID);
            if (configuration == null) {
                final ControllerException e = new ControllerException("The OSGi configuration (admin) registry was found for pid "+PID+", but a configuration could not be located/generated.  This shouldn't happen.");
                LOG.error("Error getting configuration.", e);
                throw e;
                
            }
            return configuration;
        } catch (final IOException e) {
            final ControllerException ce = new ControllerException("Failed to get configuration from OSGi configuration registry for pid "+PID+".", e);
            LOG.error("Error getting configuration.", e);
            throw new ControllerException(ce);
        }
    }

    public void setConfigurationAdmin(final ConfigurationAdmin configurationAdmin) {
        m_configurationAdmin = configurationAdmin;
    }

}
