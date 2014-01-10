package org.opennms.netmgt.api.sample;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SuppressWarnings("serial")
class JvmCollector {

	abstract static class JvmMetric extends Metric {

		public JvmMetric(String name, MetricType type) {
			super(name, type, "javavm");

		}
		
		abstract public SampleValue<?> collect();
		
	}

	Resource m_resource;

	JvmCollector.JvmMetric[] m_metrics;

	@SuppressWarnings("deprecation")
	public JvmCollector() throws UnknownHostException {
		m_resource = new Resource(new Agent(InetAddress.getLocalHost(), 80, "http"), "jvm", "resources");
		m_resource.setAttribute("version", "6");
		m_resource.setAttribute("vendor", "openjdk");

		m_metrics = new JvmCollector.JvmMetric[] {
				new JvmMetric("freeMemory", MetricType.GAUGE) {
					@Override
					public GaugeValue collect() {
						return new GaugeValue(Runtime.getRuntime().freeMemory());
					}
				},
				new JvmMetric("totalMemory", MetricType.GAUGE) {
					@Override
					public GaugeValue collect() {
						return new GaugeValue(Runtime.getRuntime().totalMemory());
					}
				},
				new JvmMetric("maxMemory", MetricType.GAUGE) {
					@Override
					public GaugeValue collect() {
						return new GaugeValue(Runtime.getRuntime().maxMemory());
					}
				},
				new JvmMetric("availableProcessors", MetricType.GAUGE) {
					@Override
					public GaugeValue collect() {
						return new GaugeValue(Runtime.getRuntime().availableProcessors());
					}
				},
				new JvmMetric("currentTimeMillis", MetricType.GAUGE) {
					@Override
					public GaugeValue collect() {
						return new GaugeValue(System.currentTimeMillis());
					}
				},
		};
	}

	public SampleSet collect() {
		
		Resource jvm = m_resource;
		
		SampleSet measurements = new SampleSet(Timestamp.now());
		System.err.println(measurements.getTimestamp());
		for(JvmCollector.JvmMetric m : m_metrics) {
			measurements.addMeasurement(jvm, m, m.collect());
		}
		
		return measurements;
	}

	public Metric getMetric(String name) {
		for(Metric m : m_metrics) {
			if (name.equals(m.getName())) {
				return m;
			}
		}
		return null;
	}

	public Resource getResource(String name) {
		if (name.equals(m_resource.getIdentifier())) {
			return m_resource;
		}
		return null;
	}
}