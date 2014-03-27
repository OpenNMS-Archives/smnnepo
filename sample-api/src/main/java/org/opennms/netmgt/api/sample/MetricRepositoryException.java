package org.opennms.netmgt.api.sample;

public class MetricRepositoryException extends Exception {
    private static final long serialVersionUID = -4185687337276251886L;

    public MetricRepositoryException() {
    }

    public MetricRepositoryException(final String message) {
        super(message);
    }

    public MetricRepositoryException(final Throwable t) {
        super(t);
    }

    public MetricRepositoryException(final String message, final Throwable t) {
        super(message, t);
    }

}
