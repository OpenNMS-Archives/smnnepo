package org.opennms.minion.controller.internal;

import org.opennms.minion.api.MinionException;

public interface ShutdownListener {
    void onShutdown() throws MinionException;
}
