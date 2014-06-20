package org.opennms.minion.controller.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="properties")
@XmlAccessorType(XmlAccessType.NONE)
public class AdaptedMapList {
    @XmlElement(name="property")
    private List<AdaptedMap> m_entries = new ArrayList<AdaptedMap>();

    public AdaptedMapList() {}
    public void addEntry(final String key, final String value) {
        m_entries.add(new AdaptedMap(key, value));
    }
    public List<AdaptedMap> getEntries() {
        return Collections.unmodifiableList(m_entries);
    }
    @Override
    public String toString() {
        return "AdaptedMapList [entries=" + m_entries + "]";
    }
}