package org.opennms.netmgt.api.sample.math;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.CounterValue;
import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.api.sample.MetricType;
import org.opennms.netmgt.api.sample.Resource;
import org.opennms.netmgt.api.sample.Results;
import org.opennms.netmgt.api.sample.Results.Row;
import org.opennms.netmgt.api.sample.Sample;
import org.opennms.netmgt.api.sample.SampleProcessor;
import org.opennms.netmgt.api.sample.SampleProcessorBuilder;
import org.opennms.netmgt.api.sample.Timestamp;

public class RollUpRatesTest {

	private final Agent m_agent = new Agent(new InetSocketAddress("127.0.0.1", 161), "SNMP", "localhost");
	private final Resource m_resource = new Resource(m_agent, "type", "name");
	private final Metric m_metric = new Metric("metric", MetricType.COUNTER, "group");


	@Test
	public void test() {
		Results input = new Results(m_resource, m_metric);
		Timestamp start = new Timestamp(0, TimeUnit.SECONDS);

		for (int i=0; i < 30000; i++) {
			input.addSample(new Sample(m_resource, m_metric, start.plus(i, TimeUnit.SECONDS), new CounterValue(i*1000)));
		}

		//printResults(input);

		SampleProcessor processor = new SampleProcessorBuilder()
				.append(new Util.TestAdapter(input))
		        .append(new Rate())
		        .append(new RollUp(200, 300, TimeUnit.SECONDS)).getProcessor();

		Results output = Util.toResults(processor);

		//printResults(output);

		assertEquals(100, output.getRows().size());

		Iterator<Row> rowIter = output.iterator();

		for (int i = 0; rowIter.hasNext(); i++) {
			Row r = rowIter.next();

			// First and last are NaN, all others 1,000
			if (i == 0 || (!rowIter.hasNext())) {
				assertTrue(r.getSample(m_metric).getValue().isNaN());
			}
			else {
				assertEquals(1000, r.getSample(m_metric).getValue().longValue());
			}
		}
	}
}
