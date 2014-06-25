package org.opennms.minion.api;

public interface MinionMessageReceiver {
    public void onMessage(MinionMessage message) throws MinionException;
}
