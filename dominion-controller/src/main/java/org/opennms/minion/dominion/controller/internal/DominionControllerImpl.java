package org.opennms.minion.dominion.controller.internal;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ShutdownRunningTask;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.ShutdownStrategy;
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
    private static final Logger LOG = LoggerFactory.getLogger(DominionControllerImpl.class);

    private String m_brokerUri;
    private String m_listenQueueName;

    private CamelContext m_camelContext;
    private ProducerTemplate m_producer;

    private StatusMessageWriter m_statusMessageWriter;
    private MinionMessageReceiver m_messageReceiver;
    private MinionMessageSender m_messageSender;

    public void start() throws MinionException {
        assert m_brokerUri           != null : "You must specify the broker URI!";
        assert m_listenQueueName     != null : "You must specify the queue to listen on for initialization messages!";
        assert m_statusMessageWriter != null : "You must pass a StatusMessageWriter to the dominion controller!";

        assertCamelContextExists();
    }

    public void stop() throws MinionException {
        LOG.debug("DominionController shutting down.");

        final List<MinionException> rethrow = new ArrayList<MinionException>();

        if (m_producer != null) {
            try {
                m_producer.stop();
                m_producer = null;
            } catch (final Exception e) {
                rethrow.add(new MinionException("Failed to shut down producer " + m_producer, e));
            }
        } 

        if (m_camelContext != null) {
            try {
                final ShutdownStrategy s = m_camelContext.getShutdownStrategy();
                s.shutdown(m_camelContext, m_camelContext.getRouteStartupOrder());
                m_camelContext.stop();
                m_camelContext = null;
            } catch (final Exception e) {
                rethrow.add(new MinionException("Failed to shut down the Camel contxt cleanly.", e));
            }
        }

        // if we have any exceptions, log them all and throw the first
        if (rethrow.size() > 0) {
            for (int i=0; i < rethrow.size(); i++) {
                final MinionException e = rethrow.get(i);
                LOG.error("stop() failed; error #{}: {}", i, e.getMessage(), e);
            }

            throw rethrow.get(0);
        }
    }

    @Override
    public void onMessage(final MinionMessage message) throws MinionException {
        if (message instanceof MinionStatusMessage) {
            LOG.debug("got status message: {}", message);
            final MinionStatusMessage statusMessage = (MinionStatusMessage) message;
            m_statusMessageWriter.write(statusMessage.getId(), statusMessage.getLocation(), statusMessage.getStatus(), statusMessage.getDate(), statusMessage.getProperties());

            final MinionInitializationMessageImpl initMessage = new MinionInitializationMessageImpl(statusMessage.getId(), 1);
            sendInitializationMessage(initMessage);
        } else {
            throw new MinionException("Unknown message type " + message.getClass().getName() + ": " + message);
        }
    }

    protected void sendInitializationMessage(final MinionInitializationMessage message) throws MinionException {
        assertMessageSenderExists();
        LOG.debug("Sending initialization message: {}", message);
        m_messageSender.sendMessage(message);
    }

    protected void assertMessageSenderExists() throws MinionException {
        if (m_messageSender == null) {
            assert m_camelContext != null : "Can't create a message sender without a camel context!";

            m_producer = m_camelContext.createProducerTemplate();
            m_messageSender = new MinionMessageSender() {
                @Override
                public void sendMessage(final MinionMessage message) throws MinionException {
                    m_producer.asyncRequestBody("direct:sendMessage", message);
                }
            };
        }
    }

    protected void assertCamelContextExists() throws MinionException {
        if (m_camelContext != null) {
            return;
        }

        final ActiveMQComponent activemq = new ActiveMQComponent();
        activemq.setBrokerURL(m_brokerUri);

        final DataFormat df;
        try {
            final JAXBContext context = JAXBContext.newInstance(MinionStatusMessageImpl.class, MinionInitializationMessageImpl.class);
            df = new JaxbDataFormat(context);
        } catch (final JAXBException e) {
            final String errorMessage = "Failed to create JAXB context for the minion controller!";
            LOG.error(errorMessage, e);
            throw new MinionException(errorMessage, e);
        }

        final Processor queueProcessor = new Processor() {
            public void process(final Exchange e) {
                final Object o = e.getIn().getBody();
                if (o instanceof MinionInitializationMessage) {
                    final MinionInitializationMessage message = (MinionInitializationMessage)o;
                    final String minionId = message.getMinionId();
                    if (minionId == null || minionId.trim().isEmpty()) {
                        return;
                    }
                    LOG.debug("Setting header to control-{}", minionId);
                    e.getIn().setHeader("CamelJmsDestinationName", "control-" + minionId);
                }
            }
        };

        final DefaultCamelContext camelContext = new DefaultCamelContext();
        camelContext.setName("dominion-controller");
        try {
            camelContext.addComponent("activemq", activemq);
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:sendMessage")
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
            camelContext.start();

            int waitfor = 30; // seconds
            while (!camelContext.isStarted() && waitfor-- > 0) {
                LOG.debug("Waiting for camel context to start...");
                Thread.sleep(1000);
            }
            m_camelContext = camelContext;
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

    public void setMessageReceiver(final MinionMessageReceiver receiver) {
        m_messageReceiver = receiver;
    }

    public void setCamelContext(final CamelContext context) {
        m_camelContext = context;
    }
}
