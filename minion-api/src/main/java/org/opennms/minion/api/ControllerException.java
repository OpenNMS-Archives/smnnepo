package org.opennms.minion.api;

public class ControllerException extends Exception {
    private static final long serialVersionUID = 1L;

    public ControllerException() {
        super();
    }

    public ControllerException(final String message) {
        super(message);
    }

    public ControllerException(final Throwable t) {
        super(t);
    }

    public ControllerException(final String message, final Throwable t) {
        super(message, t);
    }

}
