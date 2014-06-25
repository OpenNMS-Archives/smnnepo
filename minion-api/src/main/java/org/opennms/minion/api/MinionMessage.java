package org.opennms.minion.api;

public interface MinionMessage {
    public static final int CURRENT_VERSION = 1;
    public int getVersion();
}
