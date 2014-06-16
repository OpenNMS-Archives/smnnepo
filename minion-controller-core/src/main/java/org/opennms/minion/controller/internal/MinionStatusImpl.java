package org.opennms.minion.controller.internal;

import java.util.Date;

import org.opennms.minion.controller.api.IMinionStatus;

public class MinionStatusImpl implements IMinionStatus {

    private String m_id;
    private String m_location;
    private String m_status;
    private Date m_date;

    @Override
    public String getId() {
        return m_id;
    }

    public void setId(final String id) {
        m_id = id;
    }

    @Override
    public String getLocation() {
        return m_location;
    }

    public void setLocation(final String location) {
        m_location = location;
    }

    @Override
    public String getStatus() {
        return m_status;
    }

    public void setStatus(final String status) {
        m_status = status;
    }

    @Override
    public Date getDate() {
        return m_date;
    }

    public void setDate(final Date date) {
        m_date = date;
    }

}
