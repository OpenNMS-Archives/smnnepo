package org.opennms.netmgt.api.sample;

public class DeriveValue extends SampleValue<Long> {
	private static final long	serialVersionUID	= 1L;


	public DeriveValue(Long value) {
		super(value);
                throw new UnsupportedOperationException("Not yet implemented!!");
	}

	@Override
	public SampleValue<?> delta(Number other) {
		// TODO: stub
		// TODO: handle counter roll-overs here.
		return new DeriveValue(-1L);
	}

	@Override
	public SampleValue<?> add(Number other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SampleValue<?> subtract(Number other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SampleValue<?> multiply(Number other) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SampleValue<?> divide(Number object) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int intValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long longValue() {
		// TODO Auto-generated method stub
		return m_value;
	}

	@Override
	public float floatValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double doubleValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public MetricType getType() {
		return MetricType.COUNTER;
	}

	@Override
	public int compareTo(Number o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		// TODO: stubbed
		return false;
	}

	@Override
	public int hashCode() {
		// TODO: stubbed
		return 0;
	}
}
