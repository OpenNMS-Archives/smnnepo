package org.opennms.netmgt.api.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import org.junit.Test;

public class CounterValueTest {

	@Test
	public void testAdd() {
		assertEquals(1234567900L, new CounterValue(1234567800).add(100).longValue());
	}

	@Test
	public void testSubtract() {
		assertEquals(1234567800L, new CounterValue(1234567900).subtract(100).longValue());
	}

	@Test
	public void testMultiply() {
		assertEquals(
				new CounterValue(1234567900).multiply(10000),
				BigInteger.valueOf(1234567900).multiply(BigInteger.valueOf(10000)));
	}

	@Test
	public void testDivide() {
		assertEquals(
				new CounterValue(9876543210000L).divide(100000),
				BigInteger.valueOf(9876543210000L).divide(BigInteger.valueOf(100000)));
	}

	@Test
	public void testEquals() {
		assertEquals(new CounterValue(9876543210000L), BigInteger.valueOf(9876543210000L));
		assertEquals(new CounterValue(9876543210000L).hashCode(), BigInteger.valueOf(9876543210000L).hashCode());
	}

	@Test
	public void testCompare() {
		assertTrue(new CounterValue(9876543210000L).compareTo(BigInteger.valueOf(9876543210L)) > 0);
		assertEquals(0, new CounterValue(9876543210000L).compareTo(BigInteger.valueOf(9876543210000L)));
		assertTrue(new CounterValue(9876543210000L).compareTo(BigInteger.valueOf(98765432100000L)) < 0);
	}

	@Test
	public void testWrap() {
		testWrap(64);
	}

	@Test
	public void testWrap32() {
		testWrap(32);
	}

	private void testWrap(int bits) {
		if (bits != 32 && bits != 64) throw new IllegalArgumentException();

		BigInteger maxN = BigInteger.valueOf(2L).pow(bits).subtract(BigInteger.ONE);

		// Start with a value 10 less than the roll-over ceiling 
		CounterValue lastV = new CounterValue(maxN.subtract(BigInteger.TEN));
		CounterValue currV = null;

		// Increment the counter 20x, the last 10 should occur after a roll-over
		for (int i=0; i < 20; i++) {

			// Simulate counter roll-over.
			if (lastV.equals(maxN)) { 
				currV = new CounterValue(BigInteger.ZERO);
			}
			else {
				currV = lastV.add(BigInteger.ONE);
			}

			assertEquals(1L, currV.delta(lastV).longValue());

			lastV = currV;
		}
	}

	@Test
	public void testComposeDecompose() {
		CounterValue v1 = new CounterValue(123456789L);
		assertEquals(v1, CounterValue.compose(CounterValue.decompose(v1)));	
	}

}
