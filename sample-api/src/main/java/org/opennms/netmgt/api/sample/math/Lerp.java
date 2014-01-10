package org.opennms.netmgt.api.sample.math;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.opennms.netmgt.api.sample.NanValue;
import org.opennms.netmgt.api.sample.Results.Row;
import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.api.sample.Sample;
import org.opennms.netmgt.api.sample.SampleProcessor;
import org.opennms.netmgt.api.sample.SampleValue;
import org.opennms.netmgt.api.sample.Timestamp;

public class Lerp extends SampleProcessor {
	public static final TimeUnit	STD_UNITS	= TimeUnit.MILLISECONDS;

	private final Iterator<Timestamp> m_stepsIter;
	private final Timestamp m_start;
	private final Timestamp m_finish;
	private final long m_heartBeat;
	private final long m_step;
	private final TimeUnit m_stepUnits;

	private Row m_rowL = null;
	private Row m_rowR = null;
	private Row m_prevL = null;
	private Timestamp m_currentStep = null;
	private boolean initComplete = false;

	public Lerp(final Timestamp start, final Timestamp finish, final long heartBeat, final long step) {
		this(start, finish, heartBeat, step, STD_UNITS);
	}

	public Lerp(final Timestamp start, final Timestamp finish, final long heartBeat, final long step, TimeUnit stepUnits) {
		m_start = start;
		m_finish = finish;
		m_heartBeat = heartBeat;
		m_step = step;
		m_stepUnits = stepUnits;
		m_stepsIter = new Steps(m_start, m_finish, m_step, m_stepUnits).iterator();
	}

	@Override
	public boolean hasNext() {
		return (m_stepsIter.hasNext() || (m_currentStep != null));	// XXX: ???
	}

	@Override
	public Row next() {

		if (!initComplete) {
			m_rowL = nextRow();
			m_prevL = m_rowL;
			m_rowR = nextRow();
			m_currentStep = m_stepsIter.next();
			initComplete = true;
		}

		if (m_rowR == null) {
			Row r = new Row(m_rowL.getResource(), m_currentStep);

			if (m_prevL.getTimestamp().lessThan(m_currentStep)) {
				storeInterpolatedSamples(r, m_prevL, m_rowL, m_currentStep, getMetrics());	
			}
			
			stepAdvance();
			return fillMissingSamples(r);
		}

		Timestamp xL = m_rowL.getTimestamp();
		Timestamp xR = m_rowR.getTimestamp();

		/*
		 * If we have advanced beyond our current step, then we'll have to use
		 * the previous left as the left for this interpolation (this right is
		 * still guaranteed to be larger).
		 * 
		 * This condition is a given when the first row the producer gives us is
		 * larger than our first step. When this happens previous left is this
		 * left, and the interpolation will work backward.
		 */
		if (xL.greaterThan(m_currentStep)) {
			Row r = new Row(m_rowL.getResource(), m_currentStep);
			
			// When we're starting out...
			if (xL.equals(m_prevL.getTimestamp())) {
				storeInterpolatedSamples(r, m_rowL, m_rowR, m_currentStep, getMetrics());	// Backward
				stepAdvance();
			}
			else {
				storeInterpolatedSamples(r, m_prevL, m_rowL, m_currentStep, getMetrics());
				stepAdvance();
			}

			return fillMissingSamples(r);
		}

		if ((xL.lessThan(m_currentStep) || xL.equals(m_currentStep)) && (xR.greaterThan(m_currentStep) || xR.equals(m_currentStep))) {
			Row r = new Row(m_rowL.getResource(), m_currentStep);
			storeInterpolatedSamples(r, m_rowL, m_rowR, m_currentStep, getMetrics());
			advance(true);
			return fillMissingSamples(r);
		}
		else {
			advance(false);
			return next();
		}
	}

	private void storeInterpolatedSamples(Row out, Row rowL, Row rowR, Timestamp step, Collection<Metric> metrics) {
		for (Metric m : metrics) {
			Sample yL = rowL.getSample(m);
			Sample yR = rowR.getSample(m);

			SampleValue<?> value = null;

			if (yL != null && yR != null) {
				value = interpolate(step, rowL.getTimestamp(), rowR.getTimestamp(), yL, yR);
			}
			else {
				value = new NanValue();
			}

			out.addSample(new Sample(out.getResource(), m, step, value));
		}
	}

	private SampleValue<?> interpolate(Timestamp ts, Timestamp ts0, Timestamp ts1, Sample s0, Sample s1) {
		// Refuse to interpolate when points exceed threshold
		if (ts1.minus(ts0).greaterThan(new Timestamp(m_heartBeat, m_stepUnits))) {
			return null;
		}

		SampleValue<?> y0 = s0.getValue(), y1 = s1.getValue();
		long x = ts.asMillis(), x0 = ts0.asMillis(), x1 = ts1.asMillis();

		// ((x - x0) * (y1 - y0) / (x1 - x0)) + y0
		return y1.subtract(y0).multiply(x - x0).divide(x1 - x0).add(y0);
	}

	private void advance(boolean withStep) {
		if (withStep) stepAdvance();
		m_prevL = m_rowL;
		m_rowL = m_rowR;
		m_rowR = nextRow();
	}

	private void stepAdvance() {
		if (m_stepsIter.hasNext()) m_currentStep = m_stepsIter.next();
		else                       m_currentStep = null;
	}

	private Row nextRow() {
		if (getProducer().hasNext()) return getProducer().next();
		else                         return null;
	}

	@Override
	public String toString() {
		return String.format(
				"%s(start=%s, finish=%s, step=%d, step_units=%s)",
				getClass().getSimpleName(),
				m_start,
				m_finish,
				m_step,
				m_stepUnits
		);
	}

	private class Steps implements Iterable<Timestamp> {
		private final Timestamp m_start;
		private final Timestamp m_finish;
		private final long m_step;
		private final TimeUnit m_stepUnits;

		private int m_count = 0;

		private Steps(final Timestamp start, final Timestamp finish, final long step, final TimeUnit stepUnits) {
			m_start = start.atStepBoundaryStart(step, stepUnits);
			m_finish = finish.atStepBoundaryEnd(step, stepUnits);
			m_step = step;
			m_stepUnits = stepUnits;
		}

		@Override
		public Iterator<Timestamp> iterator() {
			return new Iterator<Timestamp>() {

				private Timestamp getCurrent() {
					return m_start.plus(m_count * m_step, m_stepUnits);
				}

				@Override
				public boolean hasNext() {
					return (getCurrent().compareTo(m_finish) >= 0) ? false : true;
				}

				@Override
				public Timestamp next() {
					try {
						return getCurrent();
					}
					finally {
						m_count++;
					}
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
	}
}
