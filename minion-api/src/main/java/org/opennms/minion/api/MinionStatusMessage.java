package org.opennms.minion.api;

import java.util.Date;
import java.util.Map;


public interface MinionStatusMessage {
    public abstract String getId();
    public abstract String getLocation();
    public abstract String getStatus();
    public abstract Date getDate();
    public abstract Map<String,String> getProperties();
}
