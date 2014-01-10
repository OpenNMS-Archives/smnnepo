package org.opennms.netmgt.api.sample.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.CounterValue;
import org.opennms.netmgt.api.sample.GaugeValue;
import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.api.sample.MetricType;
import org.opennms.netmgt.api.sample.Resource;
import org.opennms.netmgt.api.sample.Results;
import org.opennms.netmgt.api.sample.Results.Row;
import org.opennms.netmgt.api.sample.Sample;
import org.opennms.netmgt.api.sample.SampleProcessor;
import org.opennms.netmgt.api.sample.SampleProcessorBuilder;
import org.opennms.netmgt.api.sample.Timestamp;


public class RateTest extends Util {
	private Agent m_agent = new Agent(new InetSocketAddress("127.0.0.1", 161), "SNMP", "localhost");
	private Resource m_resource = new Resource(m_agent, "type", "name");
	private Metric m_metric = new Metric("ifInOctets", MetricType.COUNTER, "mib2-interfaces");

	private int m_step = 300;
	private TimeUnit m_timeUnits = TimeUnit.SECONDS;


	@Test
	public void testRollover32() {
		rollover(32);
	}
	
	@Test
	public void testRollover64() {
		rollover(64);
	}

	private void rollover(int bits) {
		final Timestamp ts = Timestamp.now();
		Results input = new Results(m_resource, m_metric);
		BigInteger base = BigInteger.valueOf(2).pow(bits).subtract(BigInteger.ONE);

		input.addSample(new Sample(m_resource, m_metric, ts, new CounterValue(base.subtract(BigInteger.valueOf(600L)))));
		input.addSample(new Sample(m_resource, m_metric, ts.plus(300, TimeUnit.SECONDS), new CounterValue(base.subtract(BigInteger.valueOf(300L)))));
		input.addSample(new Sample(m_resource, m_metric, ts.plus(600, TimeUnit.SECONDS), new CounterValue(base)));
		input.addSample(new Sample(m_resource, m_metric, ts.plus(900, TimeUnit.SECONDS), new CounterValue(base.add(BigInteger.valueOf(300L)))));
		input.addSample(new Sample(m_resource, m_metric, ts.plus(1200, TimeUnit.SECONDS), new CounterValue(base.add(BigInteger.valueOf(600L)))));

		//printResults(input);

		SampleProcessor processor = new SampleProcessorBuilder()
				.append(new TestAdapter(input))
				.append(new Rate()).getProcessor();
		
		Results output = toResults(processor);

		assertNotNull(output);

		//printResults(output);

		assertTrue(output.getRows().size() == 5);
		assertRateOfOne(output);
	}

	@Test
	public void testNanSamples() {
		final Timestamp ts = Timestamp.now();
		final Metric unsampled = new Metric("ifOutOctets", MetricType.COUNTER, "mib2-interfaces");

		@SuppressWarnings("serial")
		SampleProcessor entrance = new SampleProcessor() {
			// Two rows with one sample each, for one metric
			List<Row> rows = new ArrayList<Row>() {{
				Row r = new Row(m_resource, ts);
				r.addSample(new Sample(m_resource, m_metric, r.getTimestamp(), new GaugeValue(0.0d)));
				add(r);

				r = new Row(m_resource, ts.plus(300, TimeUnit.SECONDS));
				r.addSample(new Sample(m_resource, m_metric, r.getTimestamp(), new GaugeValue(300.0d)));
				add(r);
			}};

			Iterator<Row> rowsIter = rows.iterator();

			// getMetrics() returns a metric for which no samples exist (ifOutOctets)
			@Override
			public Collection<Metric> getMetrics() {
				return new ArrayList<Metric>() {{
					add(m_metric);
					add(unsampled);
				}};
			}

			@Override
			public Row next() {
				return rowsIter.next();
			}

			@Override
			public boolean hasNext() {
				return rowsIter.hasNext();
			}
		};

		SampleProcessorBuilder chain = new SampleProcessorBuilder().append(entrance).append(new Rate());
		Iterator<Row> results = chain.getProcessor();

		Row test = results.next();

		/*
		 * Expected: ifInOctets (metric) should be NaN in the first row (the
		 * first row is always NaN with Rate), and 1.0 in the second
		 * ((300.0 / 300s) = 1); ifOutOctets (unsampled) should be present in
		 * both rows, but NaN.
		 */

		assertTrue(test.getSample(m_metric).getValue().isNaN());
		assertTrue(test.getSample(unsampled).getValue().isNaN());

		test = results.next();

		assertEquals(1.0d, test.getSample(m_metric).getValue().doubleValue(), 0.0d);
		assertTrue(test.getSample(unsampled).getValue().isNaN());

		// Iterator should be dry at this point.
		assertTrue(!results.hasNext());
	}

	@Test
	public void test() {
		int numSamples = 5;
		int stepSeconds = 300;
		Timestamp start = Timestamp.now();
		Timestamp end = start.plus(stepSeconds*numSamples, TimeUnit.SECONDS);

		System.out.println("IN ------------");

		printResults(testData(start, end, m_resource, m_metric));

		System.out.println();
		System.out.println("OUT -----------");

		SampleProcessorBuilder bldr = new SampleProcessorBuilder();
		bldr.append(new Rate());

		Results results = find(m_resource, start, end, bldr, m_metric);

		printResults(results);

		assertNotNull(results);

		// There must be exactly numSamples rows
		assertTrue(results.getRows().size() == numSamples);

		Iterator<Results.Row> rowsIter = results.iterator();

		for (int i=0; rowsIter.hasNext(); i++) {
			Results.Row r = rowsIter.next();

			// There must be only 1 sample
			Collection<Sample> samples = r.getSamples();
			assertEquals(samples.size(), 1);

			// The first sample must be NaN, all subsequent 1.0d
			double expected = (i == 0) ? Double.NaN : 1.0d;
			assertSample(samples.iterator().next(), expected);
		}
	}

	/** Results where rate works out to exactly one for all samples */
	Results testData(Timestamp start, Timestamp end, Resource resource, Metric... metrics) {
		Results testData = new Results(resource, metrics);
		Timestamp current = start;

		for (int i=0; current.lessThan(end); i++) {
			for(int j = 0; j < metrics.length; j++) {
				testData.addSample(new Sample(resource, metrics[j], current, new GaugeValue(i*(j+1)*(new Double(m_step)))));
			}
			current = current.plus(m_step, m_timeUnits);
		}

		return testData;
	}

	private static void assertSample(Sample sample, double value) {
		assertEquals(sample.getValue().doubleValue(), value, 0.0d);
	}

	/** Validate single sample result sets where all values are 1.0d */
	private static void assertRateOfOne(Results results) {
		Iterator<Results.Row> rowsIter = results.iterator();

		for (int i=0; rowsIter.hasNext(); i++) {
			Results.Row r = rowsIter.next();

			// There must be only 1 sample
			Collection<Sample> samples = r.getSamples();
			assertEquals(samples.size(), 1);

			// The first sample must be NaN, all subsequent 1.0d
			double expected = (i == 0) ? Double.NaN : 1.0d;
			assertSample(samples.iterator().next(), expected);
		}
	}
}
