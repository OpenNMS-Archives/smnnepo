package org.opennms.netmgt.api.sample;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.concurrent.TimeUnit;

public final class TimeUnitAdapter extends XmlAdapter<String, TimeUnit> {

    @Override
    public String marshal(TimeUnit v) throws Exception {
        if (v != null) {
            return v.toString();
        }
        return null;
    }

    @Override
    public TimeUnit unmarshal(String v) throws Exception {
        if (v != null) {
            return TimeUnit.valueOf(v.toUpperCase());
        }
        return null;
    }

}
