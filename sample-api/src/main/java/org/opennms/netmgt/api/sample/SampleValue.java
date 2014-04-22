package org.opennms.netmgt.api.sample;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="sample-value")
public abstract class SampleValue<T extends Number> extends Number implements Comparable<Number> {
    private static final long serialVersionUID = 2L;

    public static final byte	COUNTER				= 0x01;
    public static final byte	DERIVE				= 0x02;
    public static final byte	ABSOLUTE			= 0x03;
    public static final byte	GAUGE				= 0x04;

    private final T m_value;

    public SampleValue() {
        m_value = null;
    }

    public SampleValue(T value) {
        m_value = value;
    }

    protected T getValue() {
        return m_value;
    }

    public static SampleValue<?> compose(ByteBuffer data) {
        byte dataType = data.get();

        switch (dataType) {
        case COUNTER:
            byte[] value = new byte[data.remaining()];
            data.get(value, 0, data.remaining());
            return new CounterValue(new BigInteger(value));
        case GAUGE:
            return new GaugeValue(data.getDouble());

            // TODO: DERIVE / ABSOLUTE are stubbed
        case DERIVE:
        case ABSOLUTE:
            // FIXME
            throw new UnsupportedOperationException("NOT IMPLEMENTED: FIXME!");
        default:
            throw new IllegalArgumentException(String.format("parsed unknown type descriptor from buffer (0x%x)", dataType));
        }
    }

    public static ByteBuffer decompose(SampleValue<?> value) {
        ByteBuffer res;

        switch (value.getType()) {
        case COUNTER:
            byte[] bytes = ((BigInteger)value.getValue()).toByteArray();
            res = ByteBuffer.allocate(bytes.length + 1).put(COUNTER).put(bytes);
            res.rewind();
            return res;
        case GAUGE:
            res = ByteBuffer.allocate(9).put(GAUGE).putDouble((Double)value.getValue());
            res.rewind();
            return res;

            // TODO: DERIVE / ABSOLUTE are stubbed
        case DERIVE:
            return ByteBuffer.allocate(9).put(DERIVE).putLong((Long)value.getValue());
        case ABSOLUTE:
            return ByteBuffer.allocate(9).put(ABSOLUTE).putLong((Long)value.getValue());
        default:
            throw new IllegalArgumentException(String.format("value does not correspond to a known type"));
        }
    }

    public static String toHex(final SampleValue<?> value) {
        return Hex.encodeHexString(decompose(value).array());
    }

    public static SampleValue<?> fromHex(final String value) {
        try {
            return compose(ByteBuffer.wrap(Hex.decodeHex(value.toCharArray())));
        } catch (final DecoderException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return getValue().toString();
    }

    public boolean isNaN() {
        return (getValue() == null) || (getValue().equals(Double.NaN));
    }

    public boolean lessThan(Number other) {
        return compareTo(other) < 0;
    }

    public boolean greaterThan(Number other) {
        return compareTo(other) > 0;
    }

    public abstract SampleValue<?> delta(Number other);

    public abstract SampleValue<?> subtract(Number other);

    public abstract SampleValue<?> add(Number other);

    public abstract SampleValue<?> multiply(Number other);

    public abstract SampleValue<?> divide(Number object);

    public abstract MetricType getType();

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_value == null) ? 0 : m_value.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SampleValue)) {
            return false;
        }
        @SuppressWarnings("rawtypes")
        final SampleValue other = (SampleValue) obj;
        if (m_value == null) {
            if (other.m_value != null) {
                return false;
            }
        } else if (!m_value.equals(other.m_value)) {
            return false;
        }
        return true;
    }
}
