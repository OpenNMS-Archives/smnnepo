package org.opennms.minion.dominion.controller.internal;

import org.apache.camel.Consume;
import org.opennms.minion.api.MinionException;
import org.opennms.minion.api.MinionMessage;
import org.opennms.minion.api.MinionMessageReceiver;
import org.opennms.minion.api.MinionStatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DominionControllerImpl implements MinionMessageReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(DominionControllerImpl.class);

    protected String m_endpointUri;
    private StatusMessageWriter m_statusMessageWriter;

    @Override
    @Consume(property="endpointUri")
    public void onMessage(final MinionMessage message) throws MinionException {
        if (message instanceof MinionStatusMessage) {
            LOG.debug("got status message: {}", message);
            final MinionStatusMessage statusMessage = (MinionStatusMessage) message;
            m_statusMessageWriter.write(statusMessage.getId(), statusMessage.getLocation(), statusMessage.getStatus(), statusMessage.getDate(), statusMessage.getProperties());
        } else {
            throw new MinionException("Unknown message type " + message.getClass().getName() + ": " + message);
        }
    }

    public String getEndpointUri() {
        return m_endpointUri;
    }
    
    public void setEndpointUri(final String endpointUri) {
        m_endpointUri = endpointUri;
    }
    
    public void setStatusMessageWriter(final StatusMessageWriter writer) {
        m_statusMessageWriter = writer;
    }
}
