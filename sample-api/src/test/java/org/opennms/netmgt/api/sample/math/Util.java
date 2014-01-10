package org.opennms.netmgt.api.sample.math;

import java.util.Collection;
import java.util.Iterator;

import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.api.sample.Resource;
import org.opennms.netmgt.api.sample.Results;
import org.opennms.netmgt.api.sample.Results.Row;
import org.opennms.netmgt.api.sample.Sample;
import org.opennms.netmgt.api.sample.SampleProcessor;
import org.opennms.netmgt.api.sample.SampleProcessorBuilder;
import org.opennms.netmgt.api.sample.Timestamp;


public abstract class Util {

	static class TestAdapter extends SampleProcessor {
		Resource m_resource;
		Collection<Metric> m_metrics;
		Iterator<Row> m_iterator;

		public TestAdapter(Results results) {
			m_resource = results.getResource();
			m_metrics = results.getMetrics();
			m_iterator = results.iterator();
		}

		@Override
		public boolean hasNext() {
			return m_iterator.hasNext();
		}

		@Override
		public Row next() {
			return m_iterator.next();
		}

		@Override
		public Collection<Metric> getMetrics() {
			return m_metrics;
		}

		@Override
		public Resource getResource() {
			return m_resource;
		}
	}

	Results find(Resource resource, Timestamp start, Timestamp end, SampleProcessorBuilder bldr, Metric metric) {
		TestAdapter adapter = new TestAdapter(testData(start, end, resource, metric));

		bldr.prepend(adapter);

		SampleProcessor processor = bldr.getProcessor();

		return toResults(processor);
	}

	abstract Results testData(Timestamp start, Timestamp end, Resource resource, Metric... metrics);

	static Results toResults(SampleProcessor proc) {
		Results results = new Results(proc.getResource(), proc.getMetrics().toArray(new Metric[0]));

		while(proc.hasNext()) {
			Row row = proc.next();
			for(Sample sample : row) {
				results.addSample(sample);
			}
		}

		return results;
	}

	static void printResults(Iterable<Row> r) {
		for (Results.Row row : r) {
			System.out.printf("%s (%d): ", row.getTimestamp(), row.getTimestamp().asMillis());
			for(Sample s : row) {
				System.out.printf("%s:%s", s.getMetric().getName(), s.getValue());
			}
			System.out.printf("%n");
		}
	}
}
