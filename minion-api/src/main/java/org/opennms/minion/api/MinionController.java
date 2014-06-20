package org.opennms.minion.api;


public interface MinionController {
    public static final String PID = "org.opennms.minion.controller";

    public void init() throws ControllerException;
    public String getId() throws ControllerException;
    public String getLocation() throws ControllerException;
    public MinionStatusMessage getStatus() throws ControllerException;
}
