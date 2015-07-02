package org.opennms.minion.dominion.statuswriter.dao;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.opennms.minion.api.MinionException;
import org.opennms.minion.api.StatusMessageWriter;
import org.opennms.netmgt.dao.api.MinionDao;
import org.opennms.netmgt.model.minion.OnmsMinion;

public class OnmsMinionStatusMessageWriter implements StatusMessageWriter {
    private MinionDao m_minionDao;

    @Override
    public void write(final String id, final String location, final String status, final Date lastModified, final Map<String, String> properties) throws MinionException {
        OnmsMinion minion = m_minionDao.get(id);
        if (minion == null) {
            minion = new OnmsMinion(id, location, status, lastModified);
        } else {
            // update minion attributes
            minion.setLocation(location);
            minion.setStatus(status);
            minion.setLastUpdated(lastModified);
            
            // remove any keys from the database that aren't in the new set of properties
            final Map<String,String> existingProps = minion.getProperties();
            final Set<String> keys = existingProps.keySet();
            for (final String key : keys) {
                if (!properties.containsKey(key)) {
                    minion.getProperties().remove(key);
                }
            }
        }

        // set or update all properties
        for (final Map.Entry<String,String> entry : properties.entrySet()) {
            minion.setProperty(entry.getKey(), entry.getValue());
        }

        m_minionDao.saveOrUpdate(minion);
        m_minionDao.flush();
    }

    public void setMinionDao(final MinionDao dao) {
        m_minionDao = dao;
    }
}
