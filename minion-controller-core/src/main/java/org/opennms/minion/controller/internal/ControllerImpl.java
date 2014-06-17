package org.opennms.minion.controller.internal;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.UUID;

import javax.xml.bind.JAXBContext;

import org.apache.karaf.admin.AdminService;
import org.apache.karaf.admin.Instance;
import org.apache.karaf.jms.JmsService;
import org.opennms.minion.controller.api.Controller;
import org.opennms.minion.controller.api.ControllerException;
import org.opennms.minion.controller.api.IMinionStatus;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerImpl implements Controller {
    private static final Logger LOG = LoggerFactory.getLogger(ControllerImpl.class);

    private AdminService m_adminService;
    private ConfigurationAdmin m_configurationAdmin;
    private JmsService m_jmsService;

    @Override
    public void init() throws ControllerException {
        LOG.debug("Initializing controller.");
        assert m_configurationAdmin != null : "ConfigurationAdmin is missing!";

        final String id = getId();
        if (id == null) {
            setId(UUID.randomUUID().toString());
        }
        
        final String location = getLocation();
        if (location == null) {
            throw new ControllerException("Location is not set!  Please make sure you set location='Location Name' in the " + PID + " configuration.");
        }

        String initMessageBody = null;
        final IMinionStatus status = getStatus();
        try {
            final StringWriter writer = new StringWriter();
            JAXBContext.newInstance(MinionStatusImpl.class).createMarshaller().marshal(status, writer);
            initMessageBody = writer.toString();
        } catch (final Exception e) {
            throw new ControllerException("Failed to marshal status: " + status, e);
        }

        try {
            m_jmsService.send("controllerFactory", "initialization", initMessageBody, null, null, null);
        } catch (final Exception e) {
            throw new ControllerException("Failed to send message: " + initMessageBody, e);
        }

        LOG.debug("Controller initialized. ID is {}.", getId());
    }

    @Override
    public String getId() throws ControllerException {
        final String property = loadProperty("id");
        return (property == null || property.isEmpty())? null : property;
    }

    protected void setId(final String id) throws ControllerException {
        saveProperty("id", id);
    }

    public String getLocation() throws ControllerException {
        return loadProperty("location");
    }

    protected void setLocation(final String location) throws ControllerException {
        saveProperty("location", location);
    }
    
    public URI getBrokerURI() throws ControllerException {
        final String broker = loadProperty("broker");
        try {
            return URI.create(broker);
        } catch (final IllegalArgumentException e) {
            throw new ControllerException("Invalid broker URI: " + broker, e);
        }
    }

    protected void setBrokerURI(final URI brokerUri) throws ControllerException {
        saveProperty("broker", brokerUri.toString());
    }

    @Override
    public IMinionStatus getStatus() throws ControllerException {
        final MinionStatusImpl minionStatus = new MinionStatusImpl();
        Instance rootInstance = null;
        for (final Instance instance : m_adminService.getInstances()) {
            if (instance.isRoot()) {
                rootInstance = instance;
            }
        }
        
        if (rootInstance == null) {
            throw new ControllerException("Unable to find root Karaf instance!");
        }

        minionStatus.setId(getId());
        minionStatus.setLocation(getLocation());
        try {
            minionStatus.setStatus(rootInstance.getState());
        } catch (final Exception e) {
            LOG.debug("Unable to get Karaf state.", e);
            throw new ControllerException(e);
        }
        minionStatus.setDate(new Date());
        return minionStatus;
    }

    protected String loadProperty(final String propName) throws ControllerException {
        final Configuration config = getConfiguration();
        final Dictionary<String,Object> properties = config.getProperties();
        if (properties == null) {
            return null;
        }

        final String property = (String)properties.get(propName);
        return property;
    }

    protected void saveProperty(final String key, final String value) throws ControllerException {
        final Configuration config = getConfiguration();
        final Dictionary<String,Object> properties = config.getProperties() == null? new Hashtable<String,Object>() : config.getProperties();
        properties.put(key, value);
        try {
            config.update(properties);
        } catch (final IOException e) {
            final ControllerException ce = new ControllerException("Failed to update configuration.", e);
            LOG.error("Failed to update configuration.", e);
            throw ce;
        }
    }

    protected Configuration getConfiguration() throws ControllerException {
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

    public void setAdminService(final AdminService adminService) {
        m_adminService = adminService;
    }

    public void setConfigurationAdmin(final ConfigurationAdmin configurationAdmin) {
        m_configurationAdmin = configurationAdmin;
    }

    public void setJmsService(final JmsService jmsService) throws ControllerException {
        try {
            jmsService.create("controllerFactory", "activemq", getBrokerURI().toString());
        } catch (final Exception e) {
            throw new ControllerException("Failed to create controllerBroker", e);
        }
        m_jmsService = jmsService;
    }
}
