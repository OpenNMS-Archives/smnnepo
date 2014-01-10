package org.opennms.netmgt.api.sample.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opennms.netmgt.api.sample.math.Util.printResults;
import static org.opennms.netmgt.api.sample.math.Util.toResults;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
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


public class LerpTest {

	private void assertLerp(LinkedHashMap<Long, Double> samples, LinkedHashMap<Long, Double> expected, long heartBeat, long step, TimeUnit stepUnits) {

		Agent agent = new Agent(new InetSocketAddress("127.0.0.1", 161), "SNMP", "localhost");
		Resource resource = new Resource(agent, "type", "name");
		Metric metric = new Metric("metric", MetricType.GAUGE, "group");

		Results in = new Results(resource, metric);
		Timestamp start = null, finish = null;

		for (Long ts : samples.keySet()) {
			if (start == null) {
				start = new Timestamp(ts, stepUnits);
				finish = start;
			}
			if (ts > finish.asSeconds()) {
				finish = new Timestamp(ts, stepUnits);
			}
			in.addSample(new Sample(resource, metric, new Timestamp(ts, stepUnits), new GaugeValue(samples.get(ts))));
		}

		SampleProcessorBuilder chain = new SampleProcessorBuilder().append(new Util.TestAdapter(in));
		chain.append(new Lerp(start, finish, heartBeat, step, stepUnits));

		SampleProcessor processor = chain.getProcessor();

		Results out = toResults(processor);

		printResults(out);

		// Output should contain the same number of rows as expected
		assertEquals(expected.size(), out.getRows().size());

		Iterator<Row> outIter = out.iterator();
		Iterator<Entry<Long, Double>> expectIter = expected.entrySet().iterator();

		while (outIter.hasNext()) {
			Row r = outIter.next();
			Entry<Long, Double> e = expectIter.next();
			Timestamp expectedTs = new Timestamp(e.getKey(), stepUnits);
			assertTrue(expectedTs.equals(r.getTimestamp()));
			assertEquals(e.getValue().doubleValue(), r.getSample(metric).getValue().doubleValue(), 0.0000000000001d);
		}
	}

	@Test
	public void testToStandardNoop() {
		@SuppressWarnings("serial") LinkedHashMap<Long, Double> input = new LinkedHashMap<Long, Double>(7) {{
			put(new Long(0), new Double(88.2));
			put(new Long(300), new Double(97.5));
			put(new Long(600), new Double(80.1));
			put(new Long(900), new Double(79.6));
			put(new Long(1200), new Double(85.9));
			put(new Long(1500), new Double(88.7));
			put(new Long(1800), new Double(87.8));
		}};
		
		@SuppressWarnings("serial") LinkedHashMap<Long, Double> expected = new LinkedHashMap<Long, Double>(7) {{
			put(new Long(0), new Double(88.2));
			put(new Long(300), new Double(97.5));
			put(new Long(600), new Double(80.1));
			put(new Long(900), new Double(79.6));
			put(new Long(1200), new Double(85.9));
			put(new Long(1500), new Double(88.7));
			put(new Long(1800), new Double(87.8));
		}};

		assertLerp(input, expected, 600, 300, TimeUnit.SECONDS);
	}

	@Test
	public void testToStandard() {
		@SuppressWarnings("serial") LinkedHashMap<Long, Double> input = new LinkedHashMap<Long, Double>(7) {{
			put(new Long(5), new Double(88.2));
			put(new Long(299), new Double(97.5));
			put(new Long(622), new Double(80.1));
			put(new Long(850), new Double(79.6));
			put(new Long(1207), new Double(85.9));
			put(new Long(1522), new Double(88.7));
			put(new Long(1810), new Double(87.8));
		}};

		@SuppressWarnings("serial") LinkedHashMap<Long, Double> expected = new LinkedHashMap<Long, Double>(7) {{
			put(new Long(0), new Double(88.0418367346939));
			put(new Long(300), new Double(97.4461300309598));
			put(new Long(600), new Double(81.2851393188854));
			put(new Long(900), new Double(80.4823529411765));
			put(new Long(1200), new Double(85.7764705882353));
			put(new Long(1500), new Double(88.5044444444444));
			put(new Long(1800), new Double(87.83125));
		}};

		assertLerp(input, expected, 600, 300, TimeUnit.SECONDS);
	}

	@Test
	public void testToStandardMissing() {
		@SuppressWarnings("serial") LinkedHashMap<Long, Double> input = new LinkedHashMap<Long, Double>(7) {{
			put(new Long(5), new Double(88.2));
			put(new Long(299), new Double(97.5));
			put(new Long(622), new Double(80.1));
			//put(new Long(850), new Double(79.6));
			put(new Long(1207), new Double(85.9));
			put(new Long(1522), new Double(88.7));
			put(new Long(1810), new Double(87.8));
		}};
		
		@SuppressWarnings("serial") LinkedHashMap<Long, Double> expected = new LinkedHashMap<Long, Double>(7) {{
			put(new Long(0), new Double(88.0418367346939));
			put(new Long(300), new Double(97.4461300309598));
			put(new Long(600), new Double(81.2851393188854));
			put(new Long(900), new Double(82.8562393162393));
			put(new Long(1200), new Double(85.8305982905983));
			put(new Long(1500), new Double(88.5044444444444));
			put(new Long(1800), new Double(87.83125));
		}};

		assertLerp(input, expected, 600, 300, TimeUnit.SECONDS);
	}
}
