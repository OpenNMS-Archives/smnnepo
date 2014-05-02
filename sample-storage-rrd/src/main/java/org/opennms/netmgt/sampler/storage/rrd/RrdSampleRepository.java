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

package org.opennms.netmgt.sampler.storage.rrd;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;

import org.eclipse.persistence.queries.AttributeGroup;
import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.api.sample.Resource;
import org.opennms.netmgt.api.sample.Results;
import org.opennms.netmgt.api.sample.Sample;
import org.opennms.netmgt.api.sample.SampleProcessorBuilder;
import org.opennms.netmgt.api.sample.SampleRepository;
import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.api.sample.Timestamp;
import org.opennms.netmgt.collection.api.AttributeGroupType;
import org.opennms.netmgt.collection.persistence.rrd.PersistOperationBuilder;
import org.opennms.netmgt.collection.sampler.SamplerCollectionAgent;
import org.opennms.netmgt.collection.sampler.SamplerCollectionAttribute;
import org.opennms.netmgt.collection.sampler.SamplerCollectionAttributeType;
import org.opennms.netmgt.collection.sampler.SamplerCollectionResource;
import org.opennms.netmgt.rrd.RrdException;
import org.opennms.netmgt.rrd.RrdRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RrdSampleRepository implements SampleRepository {

	private static final Logger LOG = LoggerFactory.getLogger(RrdSampleRepository.class);

	private HashMap<String, AttributeGroupType> m_groupTypeList = new HashMap<String, AttributeGroupType>();
	private HashMap<String, SamplerCollectionAttributeType> m_attribTypeList = new HashMap<String, SamplerCollectionAttributeType>();

	/**
	 * TODO: We need to figure out how to init the RRD directory
	 */
	/*
	private void initializeRrdDirs() {
		// If the RRD file repository directory does NOT already exist, create it.
		LOG.debug("initializeRrdRepository: Initializing RRD repo from JdbcCollector...");
		File f = // GET RRD REPOSITORY SOMEHOW
		if (!f.isDirectory()) {
			if (!f.mkdirs()) {
				throw new RuntimeException("Unable to create RRD file " + "repository.  Path doesn't already exist and could not make directory: " + m_jdbcCollectionDao.getConfig().getRrdRepository());
			}
		}
	}
	*/

	@Override
	public void save(SampleSet sampleSet) {
		// Create a new collection set.
		//SamplerCollectionSet collectionSet = new SamplerCollectionSet();
		//collectionSet.setCollectionTimestamp(new Date());

		// Create an RrdRepository
		RrdRepository repository = getRrdRepository();

		for (Resource resource : sampleSet.getResources()) {
			SamplerCollectionAgent agent = new SamplerCollectionAgent(resource.getAgent());
			SamplerCollectionResource collectionResource = new SamplerCollectionResource(resource);
			PersistOperationBuilder builder = new PersistOperationBuilder(repository, collectionResource, "rrdName");
			for (String groupName : sampleSet.getGroups(resource)) {
				AttributeGroup group = new AttributeGroup(groupName);
				AttributeGroupType groupType = new AttributeGroupType(groupName, AttributeGroupType.IF_TYPE_IGNORE);
				for (Sample sample : sampleSet.getSamples(resource, groupName)) {
					SamplerCollectionAttributeType attribType = new SamplerCollectionAttributeType(groupType, sample);
					SamplerCollectionAttribute attrib = new SamplerCollectionAttribute(attribType, collectionResource, sample);
					collectionResource.getGroup(groupType).addAttribute(attrib);
					builder.declareAttribute(attribType);
				}
			}
			//collectionSet.getCollectionResources().add(collectionResource);
			try {
				builder.commit();
			} catch (RrdException e) {
				LOG.error("Exception thrown when trying to store to RRD", e);
			}
		}
		//collectionSet.setStatus(ServiceCollector.COLLECTION_SUCCEEDED);
	}

	public RrdRepository getRrdRepository() {
		RrdRepository repo = new RrdRepository();
		// TODO Make these parameters configurable
		/*
		repo.setRrdBaseDir(new File(getRrdPath()));
		repo.setRraList(getRRAList(collectionName));
		repo.setStep(getStep(collectionName));
		repo.setHeartBeat((2 * getStep(collectionName)));
		*/
		repo.setRrdBaseDir(new File("/var/opennms/rrd"));
		//repo.setRraList(getRRAList(collectionName));
		repo.setRraList(Collections.singletonList("RRA:AVERAGE:0.5:1:8928"));
		repo.setStep(300);
		repo.setHeartBeat((2 * 300));
		return repo;
	}

	@Override
	public Results find(SampleProcessorBuilder builder, Timestamp start, Timestamp end, Resource resource, Metric... metrics) {
		throw new UnsupportedOperationException(getClass().getName() + ".find() is not supported");
	}

}
