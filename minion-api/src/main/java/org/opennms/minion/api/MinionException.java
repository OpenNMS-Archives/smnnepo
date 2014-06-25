package org.opennms.minion.api;

public class MinionException extends Exception {
    private static final long serialVersionUID = 1L;

    public MinionException() {
        super();
    }

    public MinionException(final String message) {
        super(message);
    }

    public MinionException(final Throwable t) {
        super(t);
    }

    public MinionException(final String message, final Throwable t) {
        super(message, t);
    }

}
