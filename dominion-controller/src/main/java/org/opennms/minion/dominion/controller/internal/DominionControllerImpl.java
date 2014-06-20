package org.opennms.minion.dominion.controller.internal;

import org.opennms.minion.api.ControllerException;
import org.opennms.minion.api.DominionController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DominionControllerImpl implements DominionController {
    private static final Logger LOG = LoggerFactory.getLogger(DominionControllerImpl.class);

    public void init() throws ControllerException {
        LOG.info("Dominion controller initialized.");
    }
}
