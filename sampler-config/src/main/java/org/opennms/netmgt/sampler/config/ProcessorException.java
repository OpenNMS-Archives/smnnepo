package org.opennms.netmgt.sampler.config;

public class ProcessorException extends Exception {
    private static final long serialVersionUID = 1842546851236658036L;

    public ProcessorException() {
    }

    public ProcessorException(final String message) {
        super(message);
    }

    public ProcessorException(final Throwable t) {
        super(t);
    }

    public ProcessorException(final String message, final Throwable t) {
        super(message, t);
    }

}
