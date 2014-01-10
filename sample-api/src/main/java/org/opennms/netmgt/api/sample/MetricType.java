package org.opennms.netmgt.api.sample;

import java.math.BigInteger;

public enum MetricType {
    COUNTER {
        @Override
        public SampleValue<?> getValue(final Number n) {
            if (n instanceof BigInteger) {
                return new CounterValue((BigInteger)n);
            } else {
                return new CounterValue(n.longValue());
            }
        }
    }, ABSOLUTE {
        @Override
        public SampleValue<?> getValue(final Number n) {
            return new AbsoluteValue(n.longValue());
        }
    }, DERIVE {
        @Override
        public SampleValue<?> getValue(final Number n) {
            return new DeriveValue(n.longValue());
        }
    }, GAUGE {
        @Override
        public SampleValue<?> getValue(final Number n) {
            return new GaugeValue(n.longValue());
        }
    };

    abstract public SampleValue<?> getValue(Number n);
}
