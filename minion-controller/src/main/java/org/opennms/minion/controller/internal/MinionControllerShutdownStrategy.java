package org.opennms.minion.controller.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultShutdownStrategy;
import org.apache.camel.spi.RouteStartupOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinionControllerShutdownStrategy extends DefaultShutdownStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(MinionControllerShutdownStrategy.class);
    private List<ShutdownListener> m_shutdownListeners = Collections.synchronizedList(new ArrayList<ShutdownListener>());

    public MinionControllerShutdownStrategy() {
        super();
        LOG.info("Initializing MinionControllerShutdownStrategy.");
    }

    @Override
    protected boolean doShutdown(final CamelContext context, final List<RouteStartupOrder> routes, final long timeout, final TimeUnit timeUnit, final boolean suspendOnly, final boolean abortAfterTimeout, final boolean forceShutdown) throws Exception {
        LOG.debug("Shutting down Camel context: {}", context);
        for (final ShutdownListener listener : m_shutdownListeners) {
            listener.onShutdown();
        }
        return super.doShutdown(context, routes, timeout, timeUnit, suspendOnly, abortAfterTimeout, forceShutdown);
    }

    void addShutdownListener(final ShutdownListener listener) {
        m_shutdownListeners.add(listener);
    }
    
    void removeShutdownListener(final ShutdownListener listener) {
        m_shutdownListeners.remove(listener);
    }
}
