package org.opennms.minion.api;


public interface MinionController {
    public static final String PID = "org.opennms.minion.controller";
    public void start() throws MinionException;
    public void stop() throws MinionException;
    public String getId() throws MinionException;
    public String getLocation() throws MinionException;
    public MinionStatusMessage getStatus() throws MinionException;
}
