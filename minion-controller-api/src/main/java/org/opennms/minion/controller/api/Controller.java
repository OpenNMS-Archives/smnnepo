package org.opennms.minion.controller.api;


public interface Controller {
    public static final String PID = "org.opennms.minion.controller";

    public void init() throws ControllerException;
    public String getId() throws ControllerException;
    public String getLocation() throws ControllerException;
    public IMinionStatus getStatus() throws ControllerException;
}
