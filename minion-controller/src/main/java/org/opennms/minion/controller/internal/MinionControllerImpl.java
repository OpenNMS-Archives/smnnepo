package org.opennms.minion.controller.internal;

import java.io.IOException;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.UUID;

import org.apache.camel.InOnly;
import org.apache.camel.Produce;
import org.apache.karaf.admin.AdminService;
import org.apache.karaf.admin.Instance;
import org.opennms.minion.api.MinionController;
import org.opennms.minion.api.MinionException;
import org.opennms.minion.api.MinionMessage;
import org.opennms.minion.api.MinionMessageSender;
import org.opennms.minion.api.MinionStatusMessage;
import org.opennms.minion.impl.MinionStatusMessageImpl;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InOnly
public class MinionControllerImpl implements MinionController {
    private static final Logger LOG = LoggerFactory.getLogger(MinionControllerImpl.class);
    private AdminService m_adminService;
    private ConfigurationAdmin m_configurationAdmin;
    private final String m_endpointUri;

    @Produce(property="endpointUri")
    protected MinionMessageSender m_messageSender;

    void setMessageSender(final MinionMessageSender sender) {
        m_messageSender = sender;
    }

    public MinionControllerImpl(final String endpointUri) {
        m_endpointUri = endpointUri;
    }

    public String getEndpointUri() {
        LOG.debug("getEndpointUri(): {}", m_endpointUri);
        return m_endpointUri;
    }

    @Override
    public void start() throws MinionException {
        LOG.debug("Initializing controller.");
        assert m_configurationAdmin != null : "ConfigurationAdmin is missing!";

        final String id = getId();
        if (id == null) {
            setId(UUID.randomUUID().toString());
        }

        final String location = getLocation();
        if (location == null) {
            throw new MinionException("Location is not set!  Please make sure you set location='Location Name' in the " + PID + " configuration.");
        }

        final MinionStatusMessage status = getStatus();
        m_messageSender.sendMessage(status);

        LOG.debug("MinionController initialized. ID is {}.", getId());
    }

    @Override
    public void stop() throws MinionException {
        LOG.debug("MinionController shutting down.");
        final MinionStatusMessageImpl message = (MinionStatusMessageImpl) getStatus();
        message.setStatus(Instance.STOPPED);
        m_messageSender.sendMessage(message);
    }

    @Override
    public String getId() throws MinionException {
        final String property = loadProperty("id");
        return (property == null || property.isEmpty())? null : property;
    }

    protected void setId(final String id) throws MinionException {
        saveProperty("id", id);
    }

    public String getLocation() throws MinionException {
        return loadProperty("location");
    }

    protected void setLocation(final String location) throws MinionException {
        saveProperty("location", location);
    }

    @Override
    public MinionStatusMessage getStatus() throws MinionException {
        final MinionStatusMessageImpl minionStatus = new MinionStatusMessageImpl(getId(), MinionMessage.CURRENT_VERSION);

        Instance rootInstance = null;
        for (final Instance instance : m_adminService.getInstances()) {
            if (instance.isRoot()) {
                rootInstance = instance;
            }
        }

        if (rootInstance == null) {
            throw new MinionException("Unable to find root Karaf instance!");
        }

        minionStatus.setLocation(getLocation());
        try {
            minionStatus.setStatus(rootInstance.getState());
        } catch (final Exception e) {
            LOG.debug("Unable to get Karaf state.", e);
            throw new MinionException(e);
        }
        minionStatus.setDate(new Date());
        return minionStatus;
    }

    protected String loadProperty(final String propName) throws MinionException {
        final Configuration config = getConfiguration();
        final Dictionary<String,Object> properties = config.getProperties();
        if (properties == null) {
            return null;
        }

        final String property = (String)properties.get(propName);
        return property;
    }

    protected void saveProperty(final String key, final String value) throws MinionException {
        final Configuration config = getConfiguration();
        final Dictionary<String,Object> properties = config.getProperties() == null? new Hashtable<String,Object>() : config.getProperties();
        properties.put(key, value);
        try {
            config.update(properties);
        } catch (final IOException e) {
            final MinionException ce = new MinionException("Failed to update configuration.", e);
            LOG.error("Failed to update configuration.", e);
            throw ce;
        }
    }

    protected Configuration getConfiguration() throws MinionException {
        try {
            final Configuration configuration = m_configurationAdmin.getConfiguration(PID);
            if (configuration == null) {
                final MinionException e = new MinionException("The OSGi configuration (admin) registry was found for pid "+PID+", but a configuration could not be located/generated.  This shouldn't happen.");
                LOG.error("Error getting configuration.", e);
                throw e;

            }
            return configuration;
        } catch (final IOException e) {
            final MinionException ce = new MinionException("Failed to get configuration from OSGi configuration registry for pid "+PID+".", e);
            LOG.error("Error getting configuration.", e);
            throw new MinionException(ce);
        }
    }

    public void setAdminService(final AdminService adminService) {
        m_adminService = adminService;
    }

    public void setConfigurationAdmin(final ConfigurationAdmin configurationAdmin) {
        m_configurationAdmin = configurationAdmin;
    }
}
