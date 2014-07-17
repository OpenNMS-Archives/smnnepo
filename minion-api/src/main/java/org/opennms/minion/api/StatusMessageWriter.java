package org.opennms.minion.api;

import java.util.Date;
import java.util.Map;

public interface StatusMessageWriter {
    public void write(final String id, final String location, final String status, final Date lastModified, final Map<String,String> properties) throws MinionException;
}
