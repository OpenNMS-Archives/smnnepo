package org.opennms.netmgt.api.sample;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Timestamp implements Comparable<Timestamp>, Serializable {
	private static final long serialVersionUID = 1L;

	public static Timestamp now() {
		return new Timestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
	}

	private final long m_time;
	private final TimeUnit m_unit;
	
	public Timestamp(long time, TimeUnit unit) {
		m_time = time;
		m_unit = unit;
	}
	
	public Timestamp(Date date) {
		m_time = date.getTime();
		m_unit = TimeUnit.MILLISECONDS;
	}

	/**
	 * @param newUnit
	 * @return the timestamp to the truncated to the passed in unit (or zero padded)
	 */
	public long convert(TimeUnit newUnit) {
		return newUnit.convert(m_time, m_unit);
	}
	
	public long asSeconds() {
		return convert(TimeUnit.SECONDS);
	}
	
	public long asMillis() {
		return convert(TimeUnit.MILLISECONDS);
	}

	public Date asDate() {
		return new Date(convert(TimeUnit.MILLISECONDS));
	}

	public boolean leftOf(Timestamp o) {
		return lessThan(o);
	}

	public boolean rightOf(Timestamp o) {
		return greaterThan(o);
	}

	public boolean lessThan(Timestamp o) {
		return compareTo(o) < 0;
	}

	public boolean greaterThan(Timestamp o) {
		return compareTo(o) > 0;
	}

	@Override
	public int compareTo(Timestamp o) {
		 long millis = convert(TimeUnit.MILLISECONDS);
		 long otherMillis = o.convert(TimeUnit.MILLISECONDS);
			
		 return millis < otherMillis ? -1 : millis > otherMillis ? 1 : 0;
			
	}

	@Override
	public boolean equals(Object other) {
		return asMillis() == ((Timestamp)other).asMillis();
	}

	@Override
	public int hashCode() {
		return Long.valueOf(asMillis()).hashCode();
	}

	@Override
	public String toString() {
		return String.format("%1$tY-%1$tb-%1$td %1$tH:%1$tM:%1$tS.%1$tL", asMillis());
	}

	/**
	 * Round a timestamp to the nearest step interval
	 * 
	 * @param stepSize size of the step interval
	 * @param stepUnits the units the timestamp is expressed in
	 * @return a rounded Timestamp
	 */
	public Timestamp roundToStep(long stepSize, TimeUnit stepUnits) {
		long skew, ts, res;

		ts = convert(stepUnits);
		skew = ts % stepSize;

		// Round up
		if (skew > (stepSize / 2)) {
			res = ts + (stepSize - skew);
		}
		// Round down
		else {
			res = ts - skew;
		}

		return new Timestamp(res, stepUnits);
	}

	public Timestamp atStepBoundaryStart(long stepSize, TimeUnit stepUnits) {
		long ts = convert(stepUnits);
		return new Timestamp((ts/stepSize)*stepSize, stepUnits);
	}

	public Timestamp atStepBoundaryEnd(long stepSize, TimeUnit stepUnits) {
		long ts = convert(stepUnits);
		return new Timestamp(((ts/stepSize)+1)*stepSize, stepUnits);
	}

	public Timestamp plus(Timestamp time) {
		return plus(time.asMillis(), TimeUnit.MILLISECONDS);
	}

	public Timestamp minus(Timestamp time) {
		return minus((int)time.asMillis(), TimeUnit.MILLISECONDS);
	}

	public Timestamp plus(long time, TimeUnit unit) {
		long duration = m_unit.convert(time, unit);
		return new Timestamp(m_time+duration, m_unit);
	}

	public Timestamp minus(int time, TimeUnit unit) {
		long duration = m_unit.convert(time, unit);
		return new Timestamp(m_time-duration, m_unit);
	}
}
