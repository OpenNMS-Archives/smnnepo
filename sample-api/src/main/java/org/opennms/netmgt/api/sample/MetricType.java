package org.opennms.netmgt.api.sample;

import java.math.BigInteger;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlEnum(String.class)
public enum MetricType {
    @XmlEnumValue(value="counter")
    COUNTER {
        @Override
        public SampleValue<?> getValue(final Number n) {
            if (n instanceof BigInteger) {
                return new CounterValue((BigInteger)n);
            } else {
                return new CounterValue(n.longValue());
            }
        }
    },
    @XmlEnumValue(value="absolute")
    ABSOLUTE {
        @Override
        public SampleValue<?> getValue(final Number n) {
            return new AbsoluteValue(n.longValue());
        }
    },
    @XmlEnumValue(value="derive")
    DERIVE {
        @Override
        public SampleValue<?> getValue(final Number n) {
            return new DeriveValue(n.longValue());
        }
    },
    @XmlEnumValue(value="gauge")
    GAUGE {
        @Override
        public SampleValue<?> getValue(final Number n) {
            return new GaugeValue(n.longValue());
        }
    };

    public abstract SampleValue<?> getValue(Number n);
}
