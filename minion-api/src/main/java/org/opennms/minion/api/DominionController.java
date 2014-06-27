package org.opennms.minion.api;

public interface DominionController {
    public static final String INITIALIZATION_QUEUE = "initialization";

    public void init() throws MinionException;
}
