package org.opennms.minion.dominion.controller.internal;

import org.opennms.minion.api.MinionException;
import org.opennms.minion.api.MinionMessage;
import org.opennms.minion.api.MinionMessageReceiver;
import org.opennms.minion.api.MinionStatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DominionControllerImpl implements MinionMessageReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(DominionControllerImpl.class);

    @Override
    public void onMessage(final MinionMessage message) throws MinionException {
        if (message instanceof MinionStatusMessage) {
            LOG.debug("got status message: {}", message);
        } else {
            throw new MinionException("Unknown message type " + message.getClass().getName() + ": " + message);
        }
    }

}
