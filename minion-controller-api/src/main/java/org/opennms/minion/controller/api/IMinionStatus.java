package org.opennms.minion.controller.api;

import java.util.Date;


public interface IMinionStatus {
    public abstract String getId();
    public abstract String getLocation();
    public abstract String getStatus();
    public abstract Date getDate();
}
