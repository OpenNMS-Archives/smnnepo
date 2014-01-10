package org.opennms.netmgt.api.sample;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class Results implements Iterable<Results.Row> {
	
	public static class Row implements Iterable<Sample> {
		Resource m_resource;
		Timestamp m_timestamp;
		Map<Metric, Sample> m_cells = new HashMap<Metric, Sample>();

		public Row(Resource resource, Timestamp timestamp) {
			m_timestamp = timestamp;
		}

		public void addSample(Sample m) {
			m_cells.put(m.getMetric(), m);
		}
		
		public Sample getSample(Metric metric) {
			return m_cells.get(metric);
		}

		public boolean containsSample(Metric metric) {
			return m_cells.containsKey(metric);
		}

		public Collection<Sample> getSamples() {
			return m_cells.values();
		}

		public Timestamp getTimestamp() {
			return m_timestamp;
		}
		
		public Iterator<Sample> iterator() {
			return m_cells.values().iterator();
		}

		public Resource getResource() {
			return m_resource;
		}

		@Override
		public String toString() {
			StringBuilder buf = new StringBuilder();
			
			buf.append(m_timestamp).append(" :");
			
			for(Sample sample : m_cells.values()) {
				buf.append(' ').append(sample.getMetric().getName()).append(":").append(sample.getValue());
			}
		
			return buf.toString();
		}
	};
	
	private Resource m_resource;
	private Metric[] m_metrics;
	private Map<Metric, SortedSet<Sample>> m_columns = new LinkedHashMap<Metric, SortedSet<Sample>>();
	private Map<Timestamp, Row> m_rows = new TreeMap<Timestamp, Row>();
	
	
	
	public Results(Resource resource, Metric... metrics) {
		m_resource = resource;
		m_metrics = metrics;
		for(Metric m : metrics) {
			m_columns.put(m, new TreeSet<Sample>());
		}
	}
	
	public Resource getResource() {
		return m_resource;
	}
	
	public List<Metric> getMetrics() {
		return Arrays.asList(m_metrics);
	}
	
	public void addSample(Sample m) {
		SortedSet<Sample> column = m_columns.get(m.getMetric());
		column.add(m);
		
		Row r = m_rows.get(m.getTimestamp());
		if (r == null) {
			r = new Row(m_resource, m.getTimestamp());
			m_rows.put(m.getTimestamp(), r);
		}
		
		r.addSample(m);
	}
	
	public SortedSet<Sample> getColumn(Metric m) {
		return m_columns.get(m);
	}

	public Collection<Row> getRows() {
		return m_rows.values();
	}
	
	@Override
	public String toString() {
		final StringBuilder buf = new StringBuilder();

		for(Row row : m_rows.values()) {
			buf.append(row).append('\n');
		}
		
		return buf.toString();
	}

	@Override
	public Iterator<Row> iterator() {
		return getRows().iterator();
	}
}
