package org.opennms.minion.controller.api;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class MapAdapter extends XmlAdapter<AdaptedMapList, Map<String,String>> {
    @Override
    public Map<String, String> unmarshal(final AdaptedMapList v) throws Exception {
        if (v == null) {
            return null;
        }
        final Map<String,String> map = new LinkedHashMap<String,String>();
        for (final AdaptedMap adaptedMap : v.getEntries()) {
            map.put(adaptedMap.getKey(), adaptedMap.getValue());
        }
        return map;
    }

    @Override
    public AdaptedMapList marshal(final Map<String, String> v) throws Exception {
        if (v == null) {
            return null;
        }
        final AdaptedMapList entries = new AdaptedMapList();
        for (final Map.Entry<String,String> entry : v.entrySet()) {
            entries.addEntry(entry.getKey(), entry.getValue());
        }
        return entries;
    }
}
