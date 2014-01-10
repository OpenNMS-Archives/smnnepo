package org.opennms.netmgt.api.sample.math;

import org.opennms.netmgt.api.sample.NanValue;
import org.opennms.netmgt.api.sample.Sample;
import org.opennms.netmgt.api.sample.SampleProcessor;
import org.opennms.netmgt.api.sample.SampleValue;
import org.opennms.netmgt.api.sample.Timestamp;
import org.opennms.netmgt.api.sample.Results.Row;

public class Rate extends SampleProcessor {
	Row m_prev;

	@Override
	public boolean hasNext() {
		return getProducer().hasNext();
	}

	@Override
	public Row next() {
		Row r = getProducer().next();
		
		Row newRow = new Row(r.getResource(), r.getTimestamp());
		for(Sample s : r) {
			Timestamp prevTime = m_prev == null ? null : m_prev.getTimestamp();
			SampleValue<?> prevVal = m_prev == null ? null : m_prev.getSample(s.getMetric()).getValue();
			long elapsed = m_prev == null ? 0 : s.getTimestamp().asMillis() - prevTime.asMillis(); 

			SampleValue<?> rate = m_prev == null ? new NanValue() : s.getValue().delta(prevVal).multiply(1000.0d).divide(elapsed);
			Sample newSample = new Sample(s.getResource(), s.getMetric(), s.getTimestamp(), rate);
			newRow.addSample(newSample);
		}

		m_prev = r;

		return fillMissingSamples(newRow);
	}

	@Override
	public String toString() {
		return String.format("%s | %s()", getProducer().toString(), getClass().getSimpleName());
	}
}