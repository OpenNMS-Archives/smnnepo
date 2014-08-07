package org.opennms.minion.dominion.controller.internal;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.opennms.minion.api.MinionInitializationMessage;

final class MinionQueueNameProcessor implements Processor {
    public void process(final Exchange e) {
        final Object o = e.getIn().getBody();
        if (o instanceof MinionInitializationMessage) {
            final MinionInitializationMessage message = (MinionInitializationMessage)o;
            final String minionId = message.getMinionId();
            if (minionId == null || minionId.trim().isEmpty()) {
                return;
            }
            DominionControllerImpl.LOG.debug("Setting header to control-{}", minionId);
            e.getIn().setHeader("CamelJmsDestinationName", "control-" + minionId);
        }
    }
}