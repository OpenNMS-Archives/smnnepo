package org.opennms.netmgt.api.sample;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class SampleSetTest {
	@SuppressWarnings("deprecation")
	@Test
	public void testSerialization() throws IOException, ClassNotFoundException {
		Timestamp time = new Timestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
		Resource resource = new Resource(new Agent(InetAddress.getLocalHost(), 80, "snmp"), "resource_type", "resource_name");
		Metric metric = new Metric("metric", MetricType.COUNTER, "metrics");
		SampleSet samplesIn = new SampleSet(time);

		for (int i=0; i < 5000; i++) {
			samplesIn.addMeasurement(resource, metric, new GaugeValue(i));
		}

		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ObjectOutputStream outStream = new ObjectOutputStream(byteStream);

		outStream.writeObject(samplesIn);
		System.out.printf("Serialized to: %d bytes%n", byteStream.size());

		ObjectInputStream inStream = new ObjectInputStream(new ByteArrayInputStream(byteStream.toByteArray()));
		SampleSet samplesOut = (SampleSet)inStream.readObject();


		assertEquals(time, samplesOut.getTimestamp());

		assertEquals(1, samplesOut.getResources().size());
		assertEquals(resource, samplesOut.getResources().iterator().next());

		assertEquals(1, samplesOut.getGroups().size());
		assertEquals("metrics", samplesOut.getGroups().iterator().next());

		assertEquals(1, samplesOut.getGroups(resource).size());
		assertEquals("metrics", samplesOut.getGroups(resource).iterator().next());

		validateSamples(samplesOut.getSamples("metrics"));
		validateSamples(samplesOut.getSamples(resource));
		validateSamples(samplesOut.getSamples(resource, "metrics"));
		validateSamples(samplesOut.getSamples());
	}

	private void validateSamples(Collection<Sample> samples) {
		Sample[] ss = new Sample[samples.size()];
		samples.toArray(ss);

		assertEquals(5000, ss.length);
		assertEquals(0.0d, ss[0].getValue().doubleValue(), 0);
		assertEquals(4999.0d, ss[ss.length-1].getValue().doubleValue(), 0);
	}
}
