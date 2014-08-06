package org.opennms.minion.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.opennms.minion.api.MinionExceptionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinionExceptionMessageImpl extends AbstractMinionMessage implements MinionExceptionMessage {
    private static final Logger LOG = LoggerFactory.getLogger(MinionExceptionMessageImpl.class);

    public MinionExceptionMessageImpl() {
    }

    public MinionExceptionMessageImpl(final Exception e) {
        if (e != null) {
            addProperty("exception.message", e.getMessage());

            try ( StringWriter sw = new StringWriter();
                  PrintWriter pw  = new PrintWriter(sw) ) {
                e.printStackTrace(pw);
                addProperty("exception.cause", sw.toString());
            } catch (final IOException ioe) {
                LOG.error("Failed to close {} while getting stack trace.", ioe.getClass().getSimpleName(), ioe);
            }
        }
    }

    @Override
    public String getMessage() {
        return getProperties().get("exception.message");
    }

    @Override
    public String getCause() {
        return getProperties().get("exception.cause");
    }
    
}
