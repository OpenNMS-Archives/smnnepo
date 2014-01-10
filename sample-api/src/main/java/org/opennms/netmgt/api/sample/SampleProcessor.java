package org.opennms.netmgt.api.sample;

import java.util.Collection;
import java.util.Iterator;

import org.opennms.netmgt.api.sample.Results.Row;

public abstract class SampleProcessor implements Iterator<Row> {
	private SampleProcessor m_producer;

	public void setProducer(SampleProcessor producer) {
		m_producer = producer;
	}

	public SampleProcessor getProducer() {
			return m_producer;
	}

	/**
	 * Returns the metrics associated with the stream of samples.
	 * 
	 * <p>
	 * Classes extending {@link SampleProcessor} should override this method
	 * when they are the entrance to the chain (the processor added with
	 * {@link SampleProcessorBuilder#prepend(SampleProcessor)}), or when they
	 * need to transform the set of metrics as part of their processing.
	 * </p>
	 * 
	 * @return the metrics associated with this sample stream.
	 */
	public Collection<Metric> getMetrics() {
		return (getProducer() != null) ? getProducer().getMetrics() : null;
	}

	public Resource getResource() {
		return (getProducer() != null) ? getProducer().getResource() : getResource();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Iterator<Row>.remove is not yet implemented.");
	}

	/**
	 * Add in NaN samples for any Metrics missing from this {@link Row}
	 * 
	 * @param row
	 *            the row to transform.
	 * @return the (same) row, with any missing samples added.
	 */
	protected Row fillMissingSamples(Row row) {
		if (getMetrics() != null) {
			for (Metric metric : getMetrics()) {
				if (!row.containsSample(metric)) {
					row.addSample(new Sample(row.getResource(), metric, row.getTimestamp(), new NanValue()));
				}
			}
		}

		return row;
	}
}
