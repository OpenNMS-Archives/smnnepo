/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2012 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.collection.sampler;

import java.io.File;

import org.opennms.netmgt.api.sample.Resource;
import org.opennms.netmgt.collection.api.CollectionResource;
import org.opennms.netmgt.collection.support.AbstractCollectionResource;
import org.opennms.netmgt.collection.support.IndexStorageStrategy;
import org.opennms.netmgt.rrd.RrdRepository;

public class SamplerCollectionResource extends AbstractCollectionResource {

	private final Resource m_resource;

	public SamplerCollectionResource(Resource resource) {
		super(new SamplerCollectionAgent(resource.getAgent()));
		m_resource = resource;
	}

	@Override
	public File getResourceDir(RrdRepository repository) {
		File rrdBaseDir = repository.getRrdBaseDir();
		// The parent directory is the ID of the agent, normally the node ID
		File nodeDir = new File(rrdBaseDir, getParent());
		if (CollectionResource.RESOURCE_TYPE_NODE.equalsIgnoreCase(m_resource.getType())) {
			return nodeDir;
		} else if (
			// TODO This case will probably never happen because interfaces are treated just like
			// generic indexed resources in the sampler object model.
			CollectionResource.RESOURCE_TYPE_IF.equalsIgnoreCase(m_resource.getType()) ||
			"ifIndex".equalsIgnoreCase(m_resource.getType()) 
		) {
			// The name field contains the interface label
			String resourceName = m_resource.getName();
			// Replace spaces, backslashes, and square brackets with underscores.
			// Remove colons.
			resourceName = resourceName.replaceAll("\\s+", "_").replaceAll(":", "").replaceAll("\\\\", "_").replaceAll("[\\[\\]]", "_");
			return new File(nodeDir, resourceName);
		} else {
			// TODO: Do we need to sanitize the resource names in the following manner?? Probably.
			/*
			File instDir = new File(typeDir, m_inst.replaceAll("\\s+", "_").replaceAll(":", "_").replaceAll("\\\\", "_").replaceAll("[\\[\\]]", "_"));
			return instDir;
			 */
			// This is an indexed resource so we need to construct a directory based on the resource type and instance
			IndexStorageStrategy strategy = new IndexStorageStrategy();
			strategy.setResourceTypeName(m_resource.getType());
			// TODO: Do we ever need to do this?
			//strategy.setParameters(m_resource.getAttributes());
			String resourcePath = new IndexStorageStrategy().getRelativePathForAttribute(getParent(), m_resource.getName());
			File resourceDir = new File(repository.getRrdBaseDir(), resourcePath);
			return resourceDir;
		}
	}

	@Override
	public String getResourceTypeName() {
		return m_resource.getType();
	}

	@Override
	public String getInterfaceLabel() {
		if (
			CollectionResource.RESOURCE_TYPE_IF.equalsIgnoreCase(m_resource.getType()) ||
			"ifIndex".equalsIgnoreCase(m_resource.getType()) 
		) {
			// If we should have an interface label ID, it will be stored in the name field
			return m_resource.getName();
		} else {
			return null;
		}
	}

	@Override
	public String getInstance() {
		if (
				CollectionResource.RESOURCE_TYPE_NODE.equalsIgnoreCase(m_resource.getType()) ||
				CollectionResource.RESOURCE_TYPE_IF.equalsIgnoreCase(m_resource.getType()) ||
				"ifIndex".equalsIgnoreCase(m_resource.getType()) 
		) {
			return null;
		} else {
			// If we should have an instance ID, it will be stored in the name field
			return m_resource.getName();
		}
	}

}
