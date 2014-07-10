package org.opennms.minion.api;

import java.util.Map;

public interface MinionMessage {
    public static final int CURRENT_VERSION = 1;
    public int getVersion();
    public Map<String,String> getProperties();
}
