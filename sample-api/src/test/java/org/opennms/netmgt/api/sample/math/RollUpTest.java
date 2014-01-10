package org.opennms.netmgt.api.sample.math;

import static org.junit.Assert.assertEquals;
import static org.opennms.netmgt.api.sample.math.Util.toResults;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.opennms.netmgt.api.sample.Agent;
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

/* https://docs.google.com/spreadsheet/ccc?key=0AomEAKhQ7epPdFFPeHRBN0YzYXFFY3pBdzhBREhrREE&usp=sharing */
public class RollUpTest {
	private final Agent m_agent = new Agent(new InetSocketAddress("127.0.0.1", 161), "SNMP", "localhost");
	private final Resource m_resource = new Resource(m_agent, "type", "name");
	private final Metric m_metric = new Metric("metric", MetricType.GAUGE, "group");	

	@Test
	public void testUnknownSamples() {
		Results in = new Results(m_resource, m_metric);

		for (int i=0; i < 6; i++) {
			// Omit the 3rd sample
			if (i != 2) {
				in.addSample(new Sample(
						m_resource,
						m_metric,
						new Timestamp((300 * i) + (i * 60), TimeUnit.SECONDS),
						new GaugeValue((i + 1) * 0.5d)));
			}
		}
	
//		printResults(in);

		SampleProcessor processor = new SampleProcessorBuilder()
				.append(new Util.TestAdapter(in))
				.append(new RollUp(400, 300, TimeUnit.SECONDS)).getProcessor();

		Results out = toResults(processor);

//		System.out.println();
//		printResults(out);

		Iterator<Row> rowsIter = out.iterator();

		Row r = rowsIter.next();
		assertEquals(300, r.getTimestamp().asSeconds());
		assertEquals(0.708333333333333d, r.getSample(m_metric).getValue().doubleValue(), 1.0e-13);

		r = rowsIter.next();
		assertEquals(600, r.getTimestamp().asSeconds());
		assertEquals(Double.NaN, r.getSample(m_metric).getValue().doubleValue(), 0.0d);

		r = rowsIter.next();
		assertEquals(900, r.getTimestamp().asSeconds());
		assertEquals(Double.NaN, r.getSample(m_metric).getValue().doubleValue(), 0.0d);

		r = rowsIter.next();
		assertEquals(1200, r.getTimestamp().asSeconds());
		assertEquals(2.08333333333333d, r.getSample(m_metric).getValue().doubleValue(), 1.0e-13);

		r = rowsIter.next();
		assertEquals(1500, r.getTimestamp().asSeconds());
		assertEquals(2.54166666666667d, r.getSample(m_metric).getValue().doubleValue(), 1.0e-13);
	}

	
	@Test
	public void testIntervalGreaterThanStep() {
		Results in = new Results(m_resource, m_metric);

		for (int i=0; i < 6; i++) {
			in.addSample(new Sample(
					m_resource,
					m_metric,
					new Timestamp((300 * i) + (i * 60), TimeUnit.SECONDS),
					new GaugeValue((i + 1) * 0.5d)));
		}

//		printResults(in);

		SampleProcessor processor = new SampleProcessorBuilder()
				.append(new Util.TestAdapter(in))
				.append(new RollUp(400, 300, TimeUnit.SECONDS)).getProcessor();

		Results out = toResults(processor);

//		System.out.println();
//		printResults(out);

		Iterator<Row> rowsIter = out.iterator();

		Row r = rowsIter.next();
		assertEquals(300, r.getTimestamp().asSeconds());
		assertEquals(0.708333333333333d, r.getSample(m_metric).getValue().doubleValue(), 1.0e-13);

		r = rowsIter.next();
		assertEquals(600, r.getTimestamp().asSeconds());
		assertEquals(1.16666666666667d, r.getSample(m_metric).getValue().doubleValue(), 1.0e-13);

		r = rowsIter.next();
		assertEquals(900, r.getTimestamp().asSeconds());
		assertEquals(1.625d, r.getSample(m_metric).getValue().doubleValue(), 1.0e-13);

		r = rowsIter.next();
		assertEquals(1200, r.getTimestamp().asSeconds());
		assertEquals(2.08333333333333d, r.getSample(m_metric).getValue().doubleValue(), 1.0e-13);

		r = rowsIter.next();
		assertEquals(1500, r.getTimestamp().asSeconds());
		assertEquals(2.54166666666667d, r.getSample(m_metric).getValue().doubleValue(), 1.0e-13);
	}

	@Test
	public void testIntervalLessThanStep() {
		Results in = new Results(m_resource, m_metric);

		for (int i=0; i < 25; i++) {
			in.addSample(new Sample(
					m_resource,
					m_metric,
					new Timestamp(55*i, TimeUnit.SECONDS),
					new GaugeValue((i+1)*0.5d)));
		}

//		printResults(in);

		SampleProcessor processor = new SampleProcessorBuilder()
				.append(new Util.TestAdapter(in))
				.append(new RollUp(200, 300, TimeUnit.SECONDS)).getProcessor();

		Results out = toResults(processor);

//		System.out.println();
//		printResults(out);

		Iterator<Row> rowsIter = out.iterator();

		Row r = rowsIter.next();
		assertEquals(300, r.getTimestamp().asSeconds());
		assertEquals(1.86363636363636d, r.getSample(m_metric).getValue().doubleValue(), 1.0e-13);

		r = rowsIter.next();
		assertEquals(600, r.getTimestamp().asSeconds());
		assertEquals(4.72727272727273, r.getSample(m_metric).getValue().doubleValue(), 1.0e-13);

		r = rowsIter.next();
		assertEquals(900, r.getTimestamp().asSeconds());
		assertEquals(7.34090909090909, r.getSample(m_metric).getValue().doubleValue(), 1.0e-13);

		r = rowsIter.next();
		assertEquals(1200, r.getTimestamp().asSeconds());
		assertEquals(10.2045454545455, r.getSample(m_metric).getValue().doubleValue(), 1.0e-13);

		r = rowsIter.next();
		assertEquals(1500, r.getTimestamp().asSeconds());
		assertEquals(Double.NaN, r.getSample(m_metric).getValue().doubleValue(), 0.0d);
	}
}
