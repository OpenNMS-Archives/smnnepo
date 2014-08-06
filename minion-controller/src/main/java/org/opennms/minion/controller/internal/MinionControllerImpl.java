package org.opennms.minion.controller.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.camel.CamelContext;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ShutdownRunningTask;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.DataFormat;
import org.apache.karaf.admin.AdminService;
import org.apache.karaf.admin.Instance;
import org.apache.karaf.admin.InstanceSettings;
import org.opennms.minion.api.Container;
import org.opennms.minion.api.ContainerConfiguration;
import org.opennms.minion.api.MinionController;
import org.opennms.minion.api.MinionException;
import org.opennms.minion.api.MinionInitializationMessage;
import org.opennms.minion.api.MinionMessage;
import org.opennms.minion.api.MinionMessageReceiver;
import org.opennms.minion.api.MinionMessageSender;
import org.opennms.minion.api.MinionStatusMessage;
import org.opennms.minion.impl.MinionExceptionMessageImpl;
import org.opennms.minion.impl.MinionInitializationMessageImpl;
import org.opennms.minion.impl.MinionStatusMessageImpl;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinionControllerImpl implements MinionController, MinionMessageReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(MinionControllerImpl.class);
    private AdminService m_adminService;
    private ConfigurationAdmin m_configurationAdmin;

    private String m_brokerUri;
    private String m_restRoot;
    private String m_sendQueueName;
    private String m_id;
    private String m_location;

    private CamelContext m_camelContext;
    private ProducerTemplate m_producer;
    private MinionMessageSender m_messageSender;
    private MinionMessageReceiver m_messageReceiver;
    private boolean m_camelContextInitialized = false;

    public MinionControllerImpl() {
    }

    @Override
    public void start() throws MinionException {
        LOG.info("Initializing MinionController.");
        assert m_adminService       != null : "AdminService is missing!";
        assert m_configurationAdmin != null : "ConfigurationAdmin is missing!";
        assert m_location           != null : "Location is missing!";
        assert m_brokerUri          != null : "Broker URI is missing!";
        assert m_restRoot           != null : "OpenNMS ReST root is missing!";
        assert m_sendQueueName      != null : "Sending queue name is missing!";

        m_id = loadProperty("id");
        if (m_id == null) {
            m_id = UUID.randomUUID().toString();
            saveProperty("id", m_id);
        }

        assertCamelContextInitialized();
        sendStartMessage();

        LOG.info("MinionController initialized. ID is {}.", m_id);
    }

    @Override
    public void stop() throws MinionException {
        LOG.info("MinionController shutting down.");
        sendStopMessage();

        // wait long enough for the message to get into activemq before bringing down the camel context
        try {
            Thread.sleep(3000);
        } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void sendStartMessage() throws MinionException {
        assertMessageSenderExists();
        m_messageSender.sendMessage(createStatusMessage(null));
    }

    @Override
    public void sendStopMessage() throws MinionException {
        assertMessageSenderExists();
        m_messageSender.sendMessage(createStatusMessage(Instance.STOPPED));
    }

    protected void assertMessageSenderExists() throws MinionException {
        if (m_messageSender == null) {
            assert m_camelContext != null : "Can't create a message sender without a camel context!";

            m_producer = m_camelContext.createProducerTemplate();
            m_messageSender = new MinionMessageSender() {
                @Override
                public void sendMessage(final MinionMessage message) throws MinionException {
                    m_producer.asyncRequestBody("seda:sendMessage", message);
                }
            };
        }
    }

    @Override
    public String getId() throws MinionException {
        return m_id;
    }

    @Override
    public String getLocation() throws MinionException {
        return m_location;
    }

    @Override
    public void onMessage(final MinionMessage message) throws MinionException {
        LOG.debug("Got minion message: {}", message);
        if (message instanceof MinionInitializationMessage) {
            final MinionInitializationMessage initMessage = (MinionInitializationMessage)message;

            final List<String> containerNames = new ArrayList<>();
            for (final Container container : initMessage.getContainers()) {
                LOG.info("(Re-)Creating '{}' container.", container.getName());
                createContainer(initMessage, container);
                containerNames.add(container.getName());
            }

            for (final Instance instance : m_adminService.getInstances()) {
                if (instance.isRoot()) { continue; }

                if (!containerNames.contains(instance.getName())) {
                    LOG.info("Destroying '{}' container.", instance.getName());
                    destroyInstance(instance);
                }
            }
        } else {
            LOG.warn("Unknown message received: {}", message);
        }
    }

    protected void createContainer(final MinionInitializationMessage initMessage, final Container container) throws MinionException {
        final List<String> featureRepositories = initMessage.getFeatureRepositories();
        final List<String> features = container.getFeatures();

        Instance instance = m_adminService.getInstance(container.getName());
        if (instance == null) {
            instance = createInstance(container.getName(), featureRepositories, features);
        }

        String state;
        try {
            state = instance.getState();
        } catch (final Exception e) {
            LOG.warn("Unable to get state from instance '{}'", instance.getName(), e);
            state = Instance.ERROR;
        }
        
        boolean doStart = false;
        switch (state) {
            case Instance.STARTED:
                // do nothing
                ;;
            case Instance.STARTING:
                // we'll wait for it to finish below
                ;;
            case Instance.ERROR:
                // recreate the instance
                destroyInstance(instance);
                instance = createInstance(container.getName(), featureRepositories, features);
                doStart = true;
                ;;
            case Instance.STOPPED:
                doStart = true;
                ;;
        }

        setConfigurationPropertiesForContainer(container, instance);

        if (doStart) {
            try {
                instance.start(null);
            } catch (final Exception e) {
                final MinionException me = new MinionException("Failed to start instance '" + container.getName() + "'.", e);
                LOG.error(me.getMessage(), me);
                sendFailure(me);
                throw me;
            }
        }
        waitForInstance(instance);
    }

    protected void setConfigurationPropertiesForContainer(final Container container, final Instance instance) throws MinionException {
        final String instanceLocation = instance.getLocation();
        for (final ContainerConfiguration config : container.getConfigurations()) {
            final String pid = config.getPid();
            try {
                final Configuration configuration = m_configurationAdmin.getConfiguration(pid);
                final Hashtable<String,Object> properties = new Hashtable<>();
                final Dictionary<String,Object> existing = configuration.getProperties();
                for (final Enumeration<String> keys = existing.keys(); keys.hasMoreElements();) {
                    final String key = keys.nextElement();
                    Object value = existing.get(key);
                    if (value != null && value.toString().contains("${karaf.base}")) {
                        value = value.toString().replace("${karaf.base}", instanceLocation);
                    }
                    properties.put(key, value);
                }
                properties.putAll(config.getProperties());
                configuration.update(properties);
            } catch (final IOException e) {
                throw new MinionException("Failed to get configuration for pid '" + pid + "' while configuring container '" + container.getName() + "'", e);
            }
        }
    }

    protected void waitForInstance(final Instance instance) throws MinionException {
        final long waitUntil = System.currentTimeMillis() + (60 * 2 * 1000); // 2 minutes
        final String instanceName = instance.getName();

        while (System.currentTimeMillis() < waitUntil) {
            try {
                final String s = instance.getState();
                if (Instance.STARTED.equals(s)) {
                    LOG.info("Instance '{}' has started.", instanceName);
                    break;
                }
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                final MinionException minionException = new MinionException("Interrupted while waiting for instance '" + instanceName + "' to start.", e);
                LOG.error(minionException.getMessage(), e);
                Thread.currentThread().interrupt();
                sendFailure(minionException);
                throw minionException;
            } catch (final Exception e) {
                final MinionException minionException = new MinionException("Failed to get state from instance '" + instanceName + "' while waiting for it to start.", e);
                LOG.error(minionException.getMessage(), e);
                sendFailure(minionException);
                throw minionException;
            }
        }
    }

    protected Instance createInstance(final String name, final List<String> featureRepositories, final List<String> features) throws MinionException {
        try {
            final Instance rootInstance = getRootInstance();
            final InstanceSettings settings = new InstanceSettings(0, 0, 0, null, null, featureRepositories, features);

            if (rootInstance == null) {
                LOG.warn("Unable to locate root instance.  Trying with 'createInstance' instead, but this will probably not work.");
                return m_adminService.createInstance(name, settings);
            } else {
                return m_adminService.cloneInstance(rootInstance.getName(), name, settings);
            }
        } catch (final Exception e) {
            LOG.error("Failed to create '{}' instance.", name, e);
            sendFailure(e);
            return null;
        }
    }

    protected void destroyInstance(final Instance instance) throws MinionException {
        if (instance != null) {
            // destroy the existing one, just in case
            try {
                instance.stop();
            } catch (final Exception e) {
                LOG.warn("Failed to stop instance '{}', trying to destroy it.", instance.getName(), e);
                try {
                    instance.destroy();
                } catch (final Exception ex) {
                    LOG.error("Failed to destroy instance '{}'.  WTF?", instance.getName(), ex);
                    sendFailure(ex);
                }
            }
        }
    }

    protected void sendFailure(final Exception e) throws MinionException {
        final MinionExceptionMessageImpl exceptionMessage = new MinionExceptionMessageImpl(e);
        m_messageSender.sendMessage(exceptionMessage);
    }

    protected boolean isStarted(final Instance instance) {
        try {
            return Instance.STARTED.equals(instance.getState());
        } catch (final Exception e) {
            return false;
        }
    }

    protected void assertMessageReceiverExists() {
        if (m_messageReceiver == null) {
            m_messageReceiver = this;
        }
    }

    protected void assertCamelContextInitialized() throws MinionException {
        if (m_camelContextInitialized) {
            LOG.trace("Camel context already initialized!");
            return;
        }

        final DataFormat df;
        try {
            final JAXBContext context = JAXBContext.newInstance(MinionStatusMessageImpl.class, MinionInitializationMessageImpl.class, MinionExceptionMessageImpl.class);
            df = new JaxbDataFormat(context);
        } catch (final JAXBException e) {
            final String errorMessage = "Failed to create JAXB context for the minion controller!";
            LOG.error(errorMessage, e);
            throw new MinionException(errorMessage, e);
        }

        try {
            if (m_camelContext instanceof DefaultCamelContext) {
                final DefaultCamelContext defaultCamelContext = (DefaultCamelContext)m_camelContext;
                int waitfor = 30; // seconds
                while (!defaultCamelContext.isStarted() && waitfor-- > 0) {
                    LOG.debug("Waiting for camel context to start...");
                    Thread.sleep(1000);
                }
            }

            m_camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("seda:sendMessage")
                    .routeId("sendMinionMessage")
                    .shutdownRunningTask(ShutdownRunningTask.CompleteAllTasks)
                    .log(LoggingLevel.DEBUG, "minion-controller: sendMinionMessage: ${body.toString()}")
                    .marshal(df)
                    .to("amq:queue:" + m_sendQueueName + "?disableReplyTo=true");

                    assertMessageReceiverExists();
                    from("amq:queue:control-" + m_id)
                    .routeId("receiveMinionMessage")
                    .shutdownRunningTask(ShutdownRunningTask.CompleteAllTasks)
                    .log(LoggingLevel.DEBUG, "minion-controller: receiveMinionMessage: ${body}")
                    .unmarshal(df)
                    .bean(m_messageReceiver, "onMessage");
                }
            });

            LOG.info("Finished initializing Camel context.");
            m_camelContextInitialized = true;
        } catch (final Exception e) {
            throw new MinionException("Failed to configure routes for minion-controller context!", e);
        }
    }

    public MinionStatusMessage createStatusMessage(final String withStatus) throws MinionException {
        final MinionStatusMessageImpl minionStatus = new MinionStatusMessageImpl(m_id, MinionMessage.CURRENT_VERSION);

        String status = withStatus;
        if (withStatus == null) {
            final Instance rootInstance = getRootInstance();

            if (rootInstance == null) {
                throw new MinionException("Unable to find root instance!");
            }

            try {
                status = rootInstance.getState();
            } catch (final Exception e) {
                throw new MinionException("Failed to get state from the root container.", e);
            }
        }

        minionStatus.setLocation(m_location);
        minionStatus.setStatus(status);
        minionStatus.setDate(new Date());
        minionStatus.addProperty("dominionBrokerUri", m_brokerUri);
        minionStatus.addProperty("dominionRestRoot", m_restRoot);
        return minionStatus;
    }

    protected Instance getRootInstance() {
        Instance rootInstance = null;
        for (final Instance instance : m_adminService.getInstances()) {
            if (instance.isRoot()) {
                rootInstance = instance;
                break;
            }
        }
        return rootInstance;
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

    public void setMessageSender(final MinionMessageSender sender) {
        m_messageSender = sender;
    }

    public void setMessageReceiver(final MinionMessageReceiver receiver) {
        m_messageReceiver = receiver;
    }

    public void setCamelContext(final CamelContext context) {
        m_camelContext = context;
    }

    public void setLocation(final String location) {
        m_location = location;
    }

    public void setDominionBrokerUri(final String brokerUri) {
        m_brokerUri = brokerUri;
    }

    public void setOpennmsRestRoot(final String restRoot) {
        m_restRoot = restRoot;
    }

    public void setSendQueueName(final String queue) {
        m_sendQueueName = queue;
    }

}
