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

import java.util.Collections;
import java.util.Date;
import java.util.Map.Entry;

import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.api.sample.Resource;
import org.opennms.netmgt.api.sample.Results;
import org.opennms.netmgt.api.sample.Sample;
import org.opennms.netmgt.api.sample.SampleProcessorBuilder;
import org.opennms.netmgt.api.sample.SampleRepository;
import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.api.sample.Timestamp;
import org.opennms.netmgt.collection.api.AttributeGroupType;
import org.opennms.netmgt.collection.api.Persister;
import org.opennms.netmgt.collection.api.PersisterFactory;
import org.opennms.netmgt.collection.api.ServiceCollector;
import org.opennms.netmgt.collection.api.ServiceParameters;
import org.opennms.netmgt.collection.sampler.SamplerCollectionAttribute;
import org.opennms.netmgt.collection.sampler.SamplerCollectionAttributeType;
import org.opennms.netmgt.collection.sampler.SamplerCollectionResource;
import org.opennms.netmgt.collection.sampler.SamplerCollectionSet;
import org.opennms.netmgt.dao.api.ResourceStorageDao;
import org.opennms.netmgt.model.ResourcePath;
import org.opennms.netmgt.rrd.RrdRepository;

public class RrdSampleRepository implements SampleRepository {

	//private static final Logger LOG = LoggerFactory.getLogger(RrdSampleRepository.class);

	private RrdRepository m_repo;

	private PersisterFactory m_persisterFactory;

	private ResourceStorageDao m_resourceStorageDao;

	@Override
	public void save(SampleSet sampleSet) {
		// Create a new collection set
		SamplerCollectionSet collectionSet = new SamplerCollectionSet();
		collectionSet.setCollectionTimestamp(new Date());

		// Create an RrdRepository
		RrdRepository repository = getRrdRepository();

		Persister persister = m_persisterFactory.createPersister(new ServiceParameters(Collections.<String,Object>emptyMap()), repository);
		for (Resource resource : sampleSet.getResources()) {
			//SamplerCollectionAgent agent = new SamplerCollectionAgent(resource.getAgent());
			SamplerCollectionResource collectionResource = new SamplerCollectionResource(resource, repository);
			for (String groupName : sampleSet.getGroups(resource)) {
				//AttributeGroup group = new AttributeGroup(groupName);
				AttributeGroupType groupType = new AttributeGroupType(groupName, AttributeGroupType.IF_TYPE_IGNORE);
				for (Sample sample : sampleSet.getSamplesForResourceAndGroup(resource, groupName)) {
					SamplerCollectionAttributeType attribType = new SamplerCollectionAttributeType(groupType, sample.getMetric());
					SamplerCollectionAttribute attrib = new SamplerCollectionAttribute(attribType, collectionResource, sample);
					collectionResource.getGroup(groupType).addAttribute(attrib);
				}
			}
			collectionSet.getCollectionResources().add(collectionResource);

            // Persist string attributes
			ResourcePath resourcePath = ResourcePath.get(collectionResource.getPath());
            for (Entry<String, String> attr : resource.getAttributes().entrySet()) {
                m_resourceStorageDao.setStringAttribute(resourcePath, attr.getKey(), attr.getValue());
            }
		}

		collectionSet.setStatus(ServiceCollector.COLLECTION_SUCCEEDED);

		// Run the BasePersister as a visitor on the resource so that the RRD values are stored
		collectionSet.visit(persister);
	}

	public RrdRepository getRrdRepository() {
		return m_repo;
	}

	public void setRrdRepository(RrdRepository repo) {
		m_repo = repo;
	}

	public PersisterFactory getPersisterFactory() {
		return m_persisterFactory;
	}

	public void setPersisterFactory(PersisterFactory persisterFactory) {
	    m_persisterFactory = persisterFactory;
	}

    public ResourceStorageDao getResourceStorageDao() {
        return m_resourceStorageDao;
    }

    public void setResourceStorageDao(ResourceStorageDao resourceStorageDao) {
        m_resourceStorageDao = resourceStorageDao;
    }

	@Override
	public Results find(SampleProcessorBuilder builder, Timestamp start, Timestamp end, Resource resource, Metric... metrics) {
		throw new UnsupportedOperationException(getClass().getName() + ".find() is not supported");
	}

}
