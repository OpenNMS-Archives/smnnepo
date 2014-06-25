package org.opennms.minion.dominion.controller.internal;

import java.util.Date;
import java.util.Map;

import org.opennms.minion.api.MinionException;

public interface StatusMessageWriter {
    public void write(final String id, final String location, final String status, final Date lastModified, final Map<String,String> properties) throws MinionException;
}
