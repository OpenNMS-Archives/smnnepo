package org.opennms.netmgt.api.sample;

public class GaugeValue extends SampleValue<Double> {

	private static final long	serialVersionUID	= 1L;


	public GaugeValue(Integer value) {
		this((double)value);
	}

	public GaugeValue(Long value) {
		this((double)value);
	}

	public GaugeValue(Double value) {
		super(value);
	}

	@Override
	public GaugeValue delta(Number other) {
		return subtract(other);
	}

	@Override
	public GaugeValue add(Number other) {
		return new GaugeValue(getValue() + getDouble(other));
	}

	@Override
	public GaugeValue subtract(Number other) {
		return new GaugeValue(getValue() - getDouble(other));
	}

	@Override
	public GaugeValue multiply(Number other) {
		return new GaugeValue(getValue() * getDouble(other));
	}

	@Override
	public GaugeValue divide(Number object) {
		return new GaugeValue(getValue() / getDouble(object));
	}

	@Override
	public int intValue() {
		return getValue().intValue();
	}

	@Override
	public long longValue() {
		return getValue().longValue();
	}

	@Override
	public float floatValue() {
		return getValue().floatValue();
	}

	@Override
	public double doubleValue() {
		return getValue();
	}

	@Override
	public MetricType getType() {
		return MetricType.GAUGE;
	}

	@Override
	public int compareTo(Number o) {
		return getValue().compareTo(getDouble(o));
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof GaugeValue) {
			return getValue().equals(((GaugeValue) o).getValue());
		}
		else if (o instanceof Double) {
			return getValue().equals(o);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return getValue().hashCode();
	}

	private Double getDouble(Number num) {
		if (num instanceof GaugeValue) {
			return ((GaugeValue)num).getValue();
		}
		else {
			return num.doubleValue();
		}
	}
}
