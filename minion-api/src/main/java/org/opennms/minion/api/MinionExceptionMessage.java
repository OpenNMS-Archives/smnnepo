package org.opennms.minion.api;


public interface MinionExceptionMessage extends MinionMessage {
    public String getMessage();
    public String getCause();
}
