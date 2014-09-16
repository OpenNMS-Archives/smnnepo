/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2007-2012 The OpenNMS Group, Inc.
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
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opennms.core.test.MockLogAppender;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.CounterValue;
import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.api.sample.MetricType;
import org.opennms.netmgt.api.sample.Resource;
import org.opennms.netmgt.api.sample.Sample;
import org.opennms.netmgt.api.sample.Timestamp;
import org.opennms.netmgt.collection.api.AttributeGroupType;
import org.opennms.netmgt.collection.api.CollectionResource;
import org.opennms.netmgt.collection.api.ServiceCollector;
import org.opennms.netmgt.collection.persistence.rrd.PersistOperationBuilder;
import org.opennms.netmgt.collection.sampler.SamplerCollectionAttribute;
import org.opennms.netmgt.collection.sampler.SamplerCollectionAttributeType;
import org.opennms.netmgt.collection.sampler.SamplerCollectionResource;
import org.opennms.netmgt.collection.sampler.SamplerCollectionSet;
import org.opennms.netmgt.rrd.RrdRepository;
import org.opennms.netmgt.rrd.RrdUtils;
import org.opennms.test.FileAnticipator;

/**
 * JUnit TestCase for storing {@link SamplerCollectionSet} instances with {@link PersistOperationBuilder}.
 *  
 * @author <a href="mailto:seth@opennms.org">Seth</a>
 * @author <a href="mailto:dj@opennms.org">DJ Gregor</a>
 */
public class SamplerCollectionSetPersistenceTest {
    private FileAnticipator m_fileAnticipator;
    private File m_snmpDirectory;
    private Agent m_agent;
    private Resource m_resource;
    private SamplerCollectionResource m_collectionResource;
    private RrdRepository m_repository;
    
    private static final String NODE_ID = "1";
    
    @Before
    public void setUp() throws Exception {
        MockLogAppender.setupLogging();
        m_fileAnticipator = new FileAnticipator();

        // Set up the collection results
        m_agent = new Agent(new InetSocketAddress(InetAddressUtils.getLocalHostAddress(), 161), "SNMP", NODE_ID);
        m_agent.setParameter("nodeId", "1");
        m_resource = new Resource(m_agent, CollectionResource.RESOURCE_TYPE_NODE, "samples", null);
        m_repository = createRrdRepository();
        m_collectionResource = new SamplerCollectionResource(m_resource, m_repository);
    }

    @After
    public void tearDown() throws Exception {
        MockLogAppender.assertNoWarningsOrGreater();
        m_fileAnticipator.deleteExpected();
        m_fileAnticipator.tearDown();
    }

    @Test
    public void testCommitWithNoDeclaredAttributes() throws Exception {
        PersistOperationBuilder builder = new PersistOperationBuilder(m_repository, m_collectionResource, "rrdName");
        builder.commit();
    }

    @Test
    public void testCommitWithDeclaredAttribute() throws Exception {
        File nodeDir = m_fileAnticipator.expecting(getSnmpRrdDirectory(), NODE_ID);
        m_fileAnticipator.expecting(nodeDir, "rrdName" + RrdUtils.getExtension());
        m_fileAnticipator.expecting(nodeDir, "rrdName" + ".meta");

        SamplerCollectionSet collectionSet = new SamplerCollectionSet();
        collectionSet.setCollectionResources(Collections.singleton(m_collectionResource));

        Sample sample = new Sample(m_resource, new Metric("mibObjectAlias", MetricType.COUNTER, "mibGroup"), Timestamp.now(), new CounterValue(100));
        AttributeGroupType groupType = new AttributeGroupType("mibGroup", AttributeGroupType.IF_TYPE_IGNORE);
        SamplerCollectionAttributeType attributeType = new SamplerCollectionAttributeType(groupType, sample.getMetric());
        SamplerCollectionAttribute attribute = new SamplerCollectionAttribute(attributeType, m_collectionResource, sample);
        m_collectionResource.getGroup(groupType).addAttribute(attribute);
        
        collectionSet.setStatus(ServiceCollector.COLLECTION_SUCCEEDED);

        PersistOperationBuilder builder = new PersistOperationBuilder(m_repository, m_collectionResource, "rrdName");
        builder.declareAttribute(attributeType);
        builder.commit();
    }

    @Test
    public void testCommitWithDeclaredAttributeAndValue() throws Exception {
        File nodeDir = m_fileAnticipator.expecting(getSnmpRrdDirectory(), NODE_ID);
        m_fileAnticipator.expecting(nodeDir, "rrdName" + RrdUtils.getExtension());
        m_fileAnticipator.expecting(nodeDir, "rrdName" + ".meta");

        SamplerCollectionSet collectionSet = new SamplerCollectionSet();
        collectionSet.setCollectionResources(Collections.singleton(m_collectionResource));

        Sample sample = new Sample(m_resource, new Metric("mibObjectAlias", MetricType.COUNTER, "mibGroup"), Timestamp.now(), new CounterValue(100));
        AttributeGroupType groupType = new AttributeGroupType("mibGroup", AttributeGroupType.IF_TYPE_IGNORE);
        SamplerCollectionAttributeType attributeType = new SamplerCollectionAttributeType(groupType, sample.getMetric());
        SamplerCollectionAttribute attribute = new SamplerCollectionAttribute(attributeType, m_collectionResource, sample);
        m_collectionResource.getGroup(groupType).addAttribute(attribute);

        PersistOperationBuilder builder = new PersistOperationBuilder(m_repository, m_collectionResource, "rrdName");
        builder.declareAttribute(attributeType);
        builder.setAttributeValue(attributeType, "100");
        builder.commit();
    }

    /*
     * TODO Does the sampler API support collecting string values?
     * 

    @Test
    public void testCommitWithDeclaredAttributeAndStringValue() throws Exception {

        MibObject mibObject = new MibObject();
        mibObject.setOid(".1.1.1.1");
        mibObject.setAlias("mibObjectAlias");
        mibObject.setType("string");
        mibObject.setInstance("0");
        mibObject.setMaxval(null);
        mibObject.setMinval(null);

        SnmpCollectionSet collectionSet = new SnmpCollectionSet(agent, collection);

        SnmpAttribueType attributeType = new StringAttributeType(resourceType, "some-collection", mibObject, new AttributeGroupType("mibGroup", AttributeGroupType.IF_TYPE_IGNORE));
        attributeType.storeResult(collectionSet, null, new SnmpResult(mibObject.getSnmpObjId(), new SnmpInstId(mibObject.getInstance()), SnmpUtils.getValueFactory().getOctetString("hello".getBytes())));

        PersistOperationBuilder builder = new PersistOperationBuilder(repository, resource, "rrdName");
        builder.declareAttribute(attributeType);
        builder.setAttributeValue(attributeType, "THIS_IS_A_STRING");
        builder.commit();
    }

    */

    private RrdRepository createRrdRepository() throws IOException {
        RrdRepository repository = new RrdRepository();
        repository.setRrdBaseDir(getSnmpRrdDirectory());
        repository.setHeartBeat(600);
        repository.setStep(300);
        repository.setRraList(Collections.singletonList("RRA:AVERAGE:0.5:1:100"));
        return repository;
    }

    private File getSnmpRrdDirectory() throws IOException {
        if (m_snmpDirectory == null) {
            m_snmpDirectory = m_fileAnticipator.tempDir("snmp"); 
        }
        return m_snmpDirectory;
    }
}
