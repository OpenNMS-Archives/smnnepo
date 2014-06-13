/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2012 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.sampler.storage.newts;

import org.opennms.netmgt.api.sample.Sample;
import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.api.sample.SampleSetDispatcher;
import org.opennms.netmgt.api.sample.SampleValue;
import org.opennms.netmgt.sampler.storage.newts.NewtsRepositoryAdapter.Batch;

import com.google.common.primitives.UnsignedLong;

public class NewtsSampleSetDispatcher implements SampleSetDispatcher {

	private NewtsRepositoryAdapter m_newtsRepository;
	
	public NewtsSampleSetDispatcher(NewtsRepositoryAdapter newtsRepository) {
		m_newtsRepository = newtsRepository;
	}
	
	private UnsignedLong toUnsignedLong(SampleValue<?> value) {
		return UnsignedLong.valueOf(value.bigIntegerValue());
	}

	@Override
	public void save(SampleSet sampleSet) {
		
		try (Batch batch = m_newtsRepository.createBatch()) {
			for(Sample sample : sampleSet.getSamples()) {
				long millis = sample.getTimestamp().asMillis();
				String resource = sample.getResource().getIdentifier();
				String metricName = sample.getMetric().getName();
				SampleValue<?> value = sample.getValue();
				switch (sample.getMetric().getType()) {
				case ABSOLUTE:
					batch.absolute(millis, resource, metricName, toUnsignedLong(value));
					break;
				case COUNTER:
					batch.counter(millis, resource, metricName, toUnsignedLong(value));
					break;
				case DERIVE:
					batch.derive(millis, resource, metricName, toUnsignedLong(value));
					break;
				case GAUGE:
					batch.gauge(millis, resource, metricName, value.doubleValue());
					break;
				}
			}
		}
		
		
	}

}
