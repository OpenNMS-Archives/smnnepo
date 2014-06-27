package org.opennms.minion.api;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class DateAdapter extends XmlAdapter<String, Date> {
    private final ThreadLocal<DateFormat> m_dateFormat = new ThreadLocal<DateFormat>() {
        protected DateFormat initialValue() {
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return dateFormat;
        }
    };

    @Override
    public Date unmarshal(final String v) throws Exception {
        if (v == null) {
            return null;
        }
        return m_dateFormat.get().parse(v);
    }

    @Override
    public String marshal(final Date v) throws Exception {
        if (v == null) {
            return null;
        }
        return m_dateFormat.get().format(v);
    }
}
