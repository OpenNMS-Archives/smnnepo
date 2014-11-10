package org.opennms.minion.dominion.controller.internal;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ShutdownRunningTask;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.DataFormat;
import org.opennms.minion.api.MinionException;
import org.opennms.minion.api.MinionInitializationMessage;
import org.opennms.minion.api.MinionMessage;
import org.opennms.minion.api.MinionMessageReceiver;
import org.opennms.minion.api.MinionMessageSender;
import org.opennms.minion.api.MinionStatusMessage;
import org.opennms.minion.api.StatusMessageWriter;
import org.opennms.minion.impl.MinionInitializationMessageImpl;
import org.opennms.minion.impl.MinionStatusMessageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DominionControllerImpl implements MinionMessageReceiver {
    static final Logger LOG = LoggerFactory.getLogger(DominionControllerImpl.class);

    private String m_brokerUri;
    private String m_listenQueueName;

    private CamelContext m_camelContext;
    private ProducerTemplate m_producer;

    private StatusMessageWriter m_statusMessageWriter;
    private MinionMessageReceiver m_messageReceiver;
    private MinionMessageSender m_messageSender;

    boolean m_camelContextInitialized = false;

    public void start() throws MinionException {
        LOG.info("DominionController starting.");
        assert m_brokerUri           != null : "You must specify the broker URI!";
        assert m_listenQueueName     != null : "You must specify the queue to listen on for initialization messages!";
        assert m_statusMessageWriter != null : "You must pass a StatusMessageWriter to the dominion controller!";

        assertCamelContextInitialized();
    }

    public void stop() throws MinionException {
        LOG.info("DominionController shutting down.");
    }

    @Override
    public void onMessage(final MinionMessage message) throws MinionException {
        if (message instanceof MinionStatusMessage) {
            LOG.debug("got status message: {}", message);
            final MinionStatusMessage statusMessage = (MinionStatusMessage) message;
            m_statusMessageWriter.write(statusMessage.getId(), statusMessage.getLocation(), statusMessage.getStatus(), statusMessage.getDate(), statusMessage.getProperties());

            final MinionInitializationMessageImpl initMessage = new MinionInitializationMessageImpl(statusMessage.getId(), 1);
            initMessage.addFeatureRepository("mvn:org.opennms.netmgt.sample/karaf/15.0.0-PJSM-SNAPSHOT/xml/minion");
            initMessage.addFeatureRepository("mvn:org.opennms.netmgt.sample/karaf/15.0.0-PJSM-SNAPSHOT/xml");
            sendInitializationMessage(initMessage);
        } else {
            throw new MinionException("Unknown message type " + message.getClass().getName() + ": " + message);
        }
    }

    protected void sendInitializationMessage(final MinionInitializationMessage message) throws MinionException {
        assertMessageSenderExists();
        LOG.info("Sending initialization message: {}", message);
        m_messageSender.sendMessage(message);
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

    protected void assertCamelContextInitialized() throws MinionException {
        if (m_camelContextInitialized) {
            return;
        }

        final DataFormat df;
        try {
            final JAXBContext context = JAXBContext.newInstance(MinionStatusMessageImpl.class, MinionInitializationMessageImpl.class);
            df = new JaxbDataFormat(context);
        } catch (final JAXBException e) {
            final String errorMessage = "Failed to create JAXB context for the minion controller!";
            LOG.error(errorMessage, e);
            throw new MinionException(errorMessage, e);
        }

        final Processor queueProcessor = new MinionQueueNameProcessor();

        try {
            if (m_camelContext instanceof DefaultCamelContext) {
                final DefaultCamelContext defaultCamelContext = (DefaultCamelContext)m_camelContext;
                int waitfor = 30; // seconds
                while (!defaultCamelContext.isStarted() && waitfor-- > 0) {
                    LOG.debug("Waiting for camel context to start...");
                    Thread.sleep(1000);
                }
            }

            final ActiveMQComponent activemq = new ActiveMQComponent();
            activemq.setBrokerURL(m_brokerUri);
            m_camelContext.addComponent("activemq", activemq);
            m_camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("seda:sendMessage")
                    .routeId("sendMinionMessage")
                    .shutdownRunningTask(ShutdownRunningTask.CompleteAllTasks)
                    .log(LoggingLevel.DEBUG, "dominion-controller: sendMinionMessage: ${body.toString()}")
                    .process(queueProcessor)
                    .marshal(df)
                    // this is transformed by setJmsQueue above to be the proper queue name
                    .to("activemq:dummy?disableReplyTo=true");

                    assertMessageReaderExists();
                    from("activemq:initialization")
                    .routeId("receiveMinionMessage")
                    .shutdownRunningTask(ShutdownRunningTask.CompleteAllTasks)
                    .log(LoggingLevel.DEBUG, "dominion-controller: receiveMinionMessage: ${body}")
                    .unmarshal(df)
                    .bean(m_messageReceiver, "onMessage");
                }
            });

            m_camelContextInitialized = true;
            LOG.info("Finished initializing Camel context.");
        } catch (final Exception e) {
            throw new MinionException("Failed to configure routes for minion-controller context!", e);
        }
    }

    private void assertMessageReaderExists() throws MinionException {
        if (m_messageReceiver == null) {
            m_messageReceiver = this;
        }
    }

    public void setBrokerUri(final String brokerUri) {
        m_brokerUri = brokerUri;
    }

    public void setListenQueueName(final String queueName) {
        m_listenQueueName = queueName;
    }

    public void setStatusMessageWriter(final StatusMessageWriter writer) {
        m_statusMessageWriter = writer;
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
}
