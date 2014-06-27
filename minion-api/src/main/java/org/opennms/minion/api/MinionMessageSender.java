package org.opennms.minion.api;

public interface MinionMessageSender {
    public void sendMessage(MinionMessage message) throws MinionException;
}
