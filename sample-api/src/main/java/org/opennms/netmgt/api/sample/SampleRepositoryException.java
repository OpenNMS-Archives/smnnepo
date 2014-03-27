package org.opennms.netmgt.api.sample;

public class SampleRepositoryException extends RuntimeException {
    private static final long serialVersionUID = 1005702812084541659L;

    public SampleRepositoryException() {
    }

    public SampleRepositoryException(final String message) {
        super(message);
    }

    public SampleRepositoryException(final Throwable t) {
        super(t);
    }

    public SampleRepositoryException(final String message, final Throwable t) {
        super(message, t);
    }

}
