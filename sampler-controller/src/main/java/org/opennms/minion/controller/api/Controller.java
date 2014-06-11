package org.opennms.minion.controller.api;

import java.util.UUID;

public interface Controller {
    public void init() throws ControllerException;
    public UUID getId() throws ControllerException;
}
