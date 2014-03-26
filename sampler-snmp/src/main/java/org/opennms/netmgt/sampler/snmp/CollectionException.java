package org.opennms.netmgt.sampler.snmp;

public class CollectionException extends Exception {
    private static final long serialVersionUID = 3238499171273052293L;

    public CollectionException() {
    }

    public CollectionException(final String message) {
        super(message);
    }

    public CollectionException(final Throwable t) {
        super(t);
    }

    public CollectionException(final String message, final Throwable t) {
        super(message, t);
    }

}
