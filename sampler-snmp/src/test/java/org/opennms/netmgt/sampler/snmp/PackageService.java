package org.opennms.netmgt.sampler.snmp;

import org.opennms.netmgt.config.collectd.Service;

public class PackageService {

	private String m_packageName;
	private Service m_service;

	public PackageService(String packageName, Service service) {
		m_packageName = packageName;
		m_service = service;
	}

	public String getPackageName() {
		return m_packageName;
	}

	public Service getService() {
		return m_service;
	}

	/*
	public int getMillis() {
		return m_millis;
	}

	public String getFilterName() {
		return m_filterName;
	}
	*/

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[packageName=" + m_packageName
			+ ", svcName=" + m_service.getName() + ", millis=" + m_service.getInterval() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((m_packageName == null) ? 0 : m_packageName.hashCode());
		result = prime * result
				+ ((m_service == null) ? 0 : m_service.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PackageService other = (PackageService) obj;
		if (m_packageName == null) {
			if (other.m_packageName != null)
				return false;
		} else if (!m_packageName.equals(other.m_packageName))
			return false;
		if (m_service == null) {
			if (other.m_service != null)
				return false;
		} else if (!m_service.equals(other.m_service))
			return false;
		return true;
	}
}
