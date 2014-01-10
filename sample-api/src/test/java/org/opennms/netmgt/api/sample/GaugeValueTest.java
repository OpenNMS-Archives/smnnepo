package org.opennms.netmgt.api.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GaugeValueTest {

	@Test
	public void testAdd() {
		assertEquals(4.2d, new GaugeValue(2.1d).add(2.1d).doubleValue(), 0.0d);
	}

	@Test
	public void testSubtract() {
		assertEquals(2.0d, new GaugeValue(3.5d).subtract(1.5d).doubleValue(), 0.0d);
	}

	@Test
	public void testMultiply() {
		assertEquals(1.5d, new GaugeValue(3.0d).multiply(0.5d).doubleValue(), 0.0d);
	}

	@Test
	public void testDivide() {
		assertEquals(0.5d, new GaugeValue(1.5d).divide(3.0d).doubleValue(), 0.0d);
	}
	
	@Test
	public void testEquals() {
		assertEquals(new GaugeValue(1234.5678d), new GaugeValue(1234.5678d));
		assertEquals(new GaugeValue(1234.5678d).doubleValue(), new GaugeValue(1234.5678d).doubleValue(), 0.0d);
		assertEquals(new GaugeValue(1234.5678d), new Double(1234.5678d));
		assertEquals(new GaugeValue(1234.5678d).hashCode(), new Double(1234.5678d).hashCode());
	}

	@Test
	public void testCompare() {
		assertTrue(new GaugeValue(0.12345d).compareTo(1.2345d) < 0);
		assertEquals(0, new GaugeValue(0.12345d).compareTo(0.12345d));
		assertTrue(new GaugeValue(1.2345d).compareTo(0.12345d) > 0);
	}

	@Test
	public void testDeltas() {
		assertEquals(-2.5d, new GaugeValue(2.5d).delta(5.0d).doubleValue(), 0.0d);
		assertEquals(2.5d, new GaugeValue(5.0d).delta(2.5d).doubleValue(), 0.0d);
	}

	@Test
	public void testComposeDecompose() {
		GaugeValue v1 = new GaugeValue(12345.6789d);
		assertEquals(v1, GaugeValue.compose(GaugeValue.decompose(v1)));	
	}
}
