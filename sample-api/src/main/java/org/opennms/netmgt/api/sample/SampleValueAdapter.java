package org.opennms.netmgt.api.sample;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class SampleValueAdapter extends XmlAdapter<StringSampleValue,SampleValue<?>> {
    @Override
    public StringSampleValue marshal(final SampleValue<?> value) throws Exception {
        return new StringSampleValue(value);
    }

    @Override
    public SampleValue<?> unmarshal(final StringSampleValue value) throws Exception {
        return SampleValue.fromHex(value.getValue());
    }
}
