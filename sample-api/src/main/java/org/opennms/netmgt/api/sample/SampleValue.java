package org.opennms.netmgt.api.sample;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.apache.commons.codec.binary.Hex;

public abstract class SampleValue<T extends Number> extends Number implements Comparable<Number> {

	private static final long	serialVersionUID	= 1L;

	public static final byte	COUNTER				= 0x01;
	public static final byte	DERIVE				= 0x02;
	public static final byte	ABSOLUTE			= 0x03;
	public static final byte	GAUGE				= 0x04;

	protected final T m_value;


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
				throw new RuntimeException("NOT IMPLEMENTED: FIXME!");	// FIXME:
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

	public static String toHex(SampleValue<?> value) {
		return Hex.encodeHexString(decompose(value).array());
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
}
