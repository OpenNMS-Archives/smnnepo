package org.opennms.netmgt.sampler.storage.newts;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLong;
import org.opennms.newts.api.Absolute;
import org.opennms.newts.api.Counter;
import org.opennms.newts.api.Derive;
import org.opennms.newts.api.Gauge;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.Timestamp;

public class NewtsRepositoryAdapter {
	
	public class Batch implements AutoCloseable {
		List<Sample> m_samples = Lists.newArrayList();
		
		private void add(Sample s) {
			m_samples.add(s);
		}
		public void gauge(long millis, String resource, String name, double value) {
			add(new Sample(Timestamp.fromEpochMillis(millis), new Resource(resource), name, MetricType.GAUGE, new Gauge(value)));
		}
		
		public void derive(long millis, String resource, String name, UnsignedLong value) {
			add(new Sample(Timestamp.fromEpochMillis(millis), new Resource(resource), name, MetricType.DERIVE, new Derive(value)));
		}
		
		public void absolute(long millis, String resource, String name, UnsignedLong value) {
			add(new Sample(Timestamp.fromEpochMillis(millis), new Resource(resource), name, MetricType.GAUGE, new Absolute(value)));
		}
		
		public void counter(long millis, String resource, String name, UnsignedLong value) {
			add(new Sample(Timestamp.fromEpochMillis(millis), new Resource(resource), name, MetricType.COUNTER, new Counter(value)));
		}
		
		public void save() {
			m_repository.insert(m_samples);
			m_samples = null;
		}
		
		@Override
		public void close() {
			save();
		}
		
		
	}
	
	SampleRepository m_repository;
	
	public NewtsRepositoryAdapter(SampleRepository repository) {
		m_repository = repository;
	}
	
	Batch createBatch() {
		return new Batch();
	}

}
