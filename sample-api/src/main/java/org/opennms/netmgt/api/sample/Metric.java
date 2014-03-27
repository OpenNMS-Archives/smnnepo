package org.opennms.netmgt.api.sample;

import java.io.Serializable;

public class Metric implements Serializable {
	private static final long serialVersionUID = 1L;

	private final String m_name;
	private final MetricType m_type;
	private final String m_group;

	public Metric(String name, MetricType type, String group) {
		m_name = name;
		m_type = type;
		m_group = group;
	}
	
	public String getName() {
		return m_name;
	}
	
	public MetricType getType() {
		return m_type;
	}

	public String getGroup() {
		return m_group;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Metric)) {
		    return false;
		}

		Metric m = (Metric)obj;

		if (!(getName().equals(m.getName()) && getType().equals(m.getType()) && getGroup().equals(m.getGroup()))) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return String.format("%s%s%s", getName(), getType(), getGroup()).hashCode();
	}

	@Override
	public String toString() {
		return String.format(
				"%s(name=%s, type=%s, group=%s)",
				getClass().getSimpleName(),
				getName(),
				getType(),
				getGroup());
	}

}
