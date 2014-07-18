package org.opennms.minion.dominion.statuswriter.logging;

import java.util.Date;
import java.util.Map;

import org.opennms.minion.api.MinionException;
import org.opennms.minion.api.StatusMessageWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingStatusMessageWriter implements StatusMessageWriter {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingStatusMessageWriter.class);

    @Override
    public void write(final String id, final String location, final String status, final Date lastModified, final Map<String, String> properties) throws MinionException {
        LOG.info("Status Message received: id={}, location={}, status={}, properties={}", id, location, status, properties);
    }

}
