package org.opennms.netmgt.api.sample.math;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.api.sample.NanValue;
import org.opennms.netmgt.api.sample.Results.Row;
import org.opennms.netmgt.api.sample.Sample;
import org.opennms.netmgt.api.sample.SampleProcessor;
import org.opennms.netmgt.api.sample.SampleValue;
import org.opennms.netmgt.api.sample.Timestamp;

/**
 * {@link SampleProcessor} to perform step-aligned roll-ups
 * 
 * <p>
 * This class produces samples which are <i>step-aligned</i>, that is to say
 * that regardless of the input sample interval, the output will occur at fixed
 * intervals equal to a specified value. For example, a step value of 300
 * seconds will yield results that occur at 5 minute intervals, where each
 * interval is aligned to the hour (:05, :10, :15, :20, etc).
 * </p>
 * 
 * <p>
 * A "heartbeat" is used to determine an upper bound on the input sample
 * interval. Late samples are considered "unknown", and when the duration of all
 * unknown samples is greater than half of the step period, that data point is
 * considered unknown as well (unknown samples have values of Double.NaN).
 * </p>
 * 
 * <p>
 * When the input contains sample intervals less than the step period, the
 * samples between steps are "rolled up" to create each data point. A roll-up is
 * simply the average of the intervening samples.
 * </p>
 */
public class RollUp extends SampleProcessor {
	private final long m_heartBeat;
	private final long m_step;
	private final TimeUnit m_timeUnits;

	private boolean m_isInitialized = false;
	private Timestamp m_currStepFloor;
	private PrimaryDataPoint m_currPdp;
	private Row m_currRow;
	private Row m_prevRow;

	/**
	 * Create a new RollUp processor.
	 * 
	 * @param heartBeat
	 *            maximum period between input samples
	 * @param step
	 *            the sample interval to output
	 * @param timeUnits
	 *            the units of time to use for {@code step} and
	 *            {@code heartBeat}
	 */
	public RollUp(final long heartBeat, final long step, final TimeUnit timeUnits) {
		m_heartBeat = heartBeat;
		m_step = step;
		m_timeUnits = timeUnits;
	}

	/** {@inheritDoc} */
	@Override
	public boolean hasNext() {
		return getProducer().hasNext();
	}

	/** {@inheritDoc} */
	@Override
	public Row next() {
		Row resultRow = null;

		while (consume()) {
			resultRow = m_currPdp.addSamples(m_currRow);

			// This step is over, time to exit the loop
			if (resultRow != null) {
				m_currStepFloor = nextStep();
				m_currPdp = getNextPdp();
				break;
			}
		}

		return (resultRow != null) ? resultRow : getNanSamples(nextStep());
	}

	/*
	 * Advance one row through the producer stream, as needed. Returns true on
	 * success, false where there is nothing left to be consumed.
	 */
	private boolean consume() {
		if (!hasNext()) return false;
		if (!m_isInitialized) return initialize();

		/*
		 * If the current row occurred later than the current step's ceiling,
		 * then do not advance; There is more work to do at this position.
		 * 
		 * NOTE: This happens whenever there is a many-to-one relationship
		 * between steps and samples. For example, if the heart beat sample
		 * interval is high relative to the step interval, or if there are
		 * missing samples.
		 */
		if (!m_currRow.getTimestamp().greaterThan(nextStep())) {
			m_prevRow = m_currRow;
			m_currRow = getProducer().next();
		}

		return true;
	}

	/*
	 * Perform one-time start up initialization. Return true if successful,
	 * false if initialization failed and the end of the stream has been
	 * reached.
	 */
	private boolean initialize() {
		if (!m_isInitialized) {
			try {
				m_prevRow = getProducer().next();
				m_currRow = getProducer().next();
			}
			catch (NoSuchElementException err) {
				return false;
			}

			// The first step period starts at the boundary-start of the first sample consumed.
			m_currStepFloor = m_prevRow.getTimestamp().atStepBoundaryStart(m_step, m_timeUnits);
			m_currPdp = new PrimaryDataPoint(m_currStepFloor, nextStep(), m_prevRow);
			m_isInitialized = true;
		}
		return true;
	}

	/*
	 * Create a new PrimaryDataPoint object based on current state.
	 * 
	 * Each new PDP will be created for the step period occurring directly after
	 * the previous one. When the time-stamp of the current row is greater than
	 * the ceiling of this new step period, then the new PDP is initialized with
	 * the same starting row as the last. This happens whenever there is a
	 * many-to-one relationship between steps and samples. For example, if the
	 * heart beat sample interval is high relative to the step interval, or if
	 * there are missing samples.
	 */
	private PrimaryDataPoint getNextPdp() {
		Row first = (m_currRow.getTimestamp().greaterThan(nextStep())) ? m_prevRow : m_currRow;
		return new PrimaryDataPoint(m_currStepFloor, nextStep(), first);
	}


	/* Return a row with samples for all required metrics, values are Double.NaN. */
	private Row getNanSamples(Timestamp time) {
		Row row = new Row(getResource(), time);
		for (Metric m : getMetrics()) row.addSample(new Sample(row.getResource(), m, row.getTimestamp(), new NanValue()));
		return row;
	}

	/* The next step value; Ceiling of the current step period */
	private Timestamp nextStep() {
		return nextStep(m_currStepFloor);
	}

	/* */
	private Timestamp nextStep(Timestamp time) {
		return time.plus(m_step, m_timeUnits);
	}

	@Override
	public String toString() {
		return String.format(
				"%s | %s(%d, %d, %s)",
				getProducer().toString(),
				getClass().getSimpleName(),
				m_heartBeat,
				m_step,
				m_timeUnits);
	}

	/**
	 * A class to calculate a "Primary Data Point", or PDP (borrowed rrdtool
	 * terminology), from an arbitrary number of sample rows.
	 */
	private class PrimaryDataPoint {
		private final Timestamp m_finish;
		private final long m_heartBeatMs = TimeUnit.MILLISECONDS.convert(m_heartBeat, m_timeUnits);

		private Row m_prevAveraged;
		private int m_rowCount = 0;
		private int m_unknownMs = 0;


		/**
		 * Create a new PrimaryDataPoint.
		 * 
		 * @param start starting time of this data point
		 * @param finish ending time of this data point
		 * @param first initial samples row
		 */
		private PrimaryDataPoint(final Timestamp start, final Timestamp finish, Row first) {
			m_finish = finish;

			// Seed bucket with the first row
			m_prevAveraged = first;
			m_rowCount += 1;
		}

		/*
		 * Add sample rows to this PDP.
		 * 
		 * When the step period is complete the Row returned will contain the
		 * PDP results, otherwise it will be null.
		 */
		private Row addSamples(Row row) {

			m_rowCount += 1;

			// If this sample interval exceeds heartbeat, than this interval is unknown
			long lastInterval = row.getTimestamp().asMillis() - m_prevAveraged.getTimestamp().asMillis();
			if (lastInterval > m_heartBeatMs) m_unknownMs += lastInterval;


			Row currAveraged = getCumulativeAverageSamples(row);

			// Show time
			if (row.getTimestamp().greaterThan(m_finish) || row.getTimestamp().equals(m_finish)) {
				Row result = new Row(getResource(), m_finish);

				// The whole step is NaN if unknown time is more than half the step period.
				if (m_unknownMs <= (TimeUnit.MILLISECONDS.convert(m_step, m_timeUnits) / 2)) {
					addInterpolatedSamples(result, m_prevAveraged, currAveraged);
				}
				else {
					result = getNanSamples(m_finish);
				}
				
				return result;
			}

			m_prevAveraged = currAveraged;

			return null;
		}

		/*
		 * Return a new row whose values are the cumulative averages of this
		 * row, and any previous rows in this step period. When the sample
		 * interval is greater than or equal to the step period, this should
		 * result in an identical row (because the set of values we're averaging
		 * is exactly one in length).
		 */
		private Row getCumulativeAverageSamples(Row current) {
			Row row = new Row(getResource(), current.getTimestamp());
			for (Metric m : getMetrics()) {
				row.addSample(getCumulativeAverageSample(m_prevAveraged.getSample(m), current.getSample(m)));
			}
			return row;
		}

		private Sample getCumulativeAverageSample(Sample previous, Sample current) {
			SampleValue<?> newAvg = previous.getValue().multiply(m_rowCount - 1).add(current.getValue()).divide(m_rowCount);
			return new Sample(current.getResource(), current.getMetric(), current.getTimestamp(), newAvg);
		}

		private void addInterpolatedSamples(Row row, Row row0, Row row1) {
			for (Metric m : getMetrics()) {
				Sample y0 = row0.getSample(m);
				Sample y1 = row1.getSample(m);

				SampleValue<?> value = null;

				if (y0 != null && y1 != null) {
					value = interpolate(row.getTimestamp(), row0.getTimestamp(), row1.getTimestamp(), y0, y1);
				}
				else {
					value = new NanValue();
				}

				row.addSample(new Sample(getResource(), m, row.getTimestamp(), value));
			}
		}

		private SampleValue<?> interpolate(Timestamp x, Timestamp x0, Timestamp x1, Sample y0, Sample y1) {
			return interpolate(x.asMillis(), x0.asMillis(), x1.asMillis(), y0.getValue(), y1.getValue());
		}

		private SampleValue<?> interpolate(long x, long x0, long x1, SampleValue<?> y0, SampleValue<?> y1) {
			// ((x - x0) * (y1 - y0) / (x1 - x0)) + y0
			return y1.subtract(y0).multiply(x - x0).divide(x1 - x0).add(y0);
		}
	}
}






