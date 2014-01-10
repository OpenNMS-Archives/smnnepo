package org.opennms.netmgt.api.sample;

import java.util.Iterator;


public class SampleMath {
	
	
	interface Processor {
		Sample process(Sample s);
	}
	
	private Processor smallest() {
		return new Processor() {
			Sample m_smallest;

			public Sample process(Sample s) {
				if (s.getValue().lessThan(m_smallest.getValue())) {
					m_smallest = s;
				}
				return m_smallest;
			}
		};
	}
	
	private Iterator<Sample> apply(final Processor processor, final Iterator<Sample> samples) {
		return new Iterator<Sample>() {

			@Override
			public boolean hasNext() {
				return (samples.hasNext());
			}

			@Override
			public Sample next() {
				return processor.process(samples.next());
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Iterator<Sample>.remove is not supported.");
			}
		};
	}
	
	public Iterator<Sample> smallestSoFar(final Iterator<Sample> samples) {
		return apply(smallest(), samples);
	}
	

}
