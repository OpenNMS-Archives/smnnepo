package org.opennms.minion.dominion.controller.internal;

import java.io.StringReader;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.opennms.minion.api.MinionStatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitializationListener implements MessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(InitializationListener.class);

    public InitializationListener() {
        LOG.info("Starting InitializationListener.");
    }

    @Override
    public void onMessage(final Message message) {
        if (message instanceof TextMessage) {
            LOG.info("Initialization received: {}", message);
            final TextMessage textMessage = (TextMessage)message;

            StringReader reader = null;
            try {
                final String text = textMessage.getText();
                reader = new StringReader(text);
                final Unmarshaller unmarshaller = JAXBContext.newInstance(MinionStatusMessage.class).createUnmarshaller();
                final MinionStatusMessage m = (MinionStatusMessage)unmarshaller.unmarshal(reader);
                LOG.info("Successfully unmarshalled: {}", m);
            } catch (final Exception e) {
                throw new RuntimeException("Failed to parse initialization message.", e);
            } finally {
                reader.close();
            }
        } else {
            LOG.warn("Unhandled message: {}", message);
        }
    }

}
