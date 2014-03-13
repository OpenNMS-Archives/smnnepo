package org.opennms.netmgt.api.sample;

import java.io.Serializable;

public class Sample implements Comparable<Sample>, Serializable {
	private static final long serialVersionUID = 1L;

	private final Resource  m_resource;
	private final Metric    m_metric;
	private final Timestamp m_timestamp;
	private final SampleValue<?>    m_value;
	
	
	public Sample(Resource resource, Metric metric, Timestamp timestamp, SampleValue<?> value) {
		m_resource = resource;
		m_metric = metric;
		m_timestamp = timestamp;
		m_value = value;
	}


	public Resource getResource() {
		return m_resource;
	}


	public Metric getMetric() {
		return m_metric;
	}


	public Timestamp getTimestamp() {
		return m_timestamp;
	}


	public SampleValue<?> getValue() {
		return m_value;
	}


	@Override
	public int compareTo(Sample o) {
		return getTimestamp().compareTo(o.getTimestamp());
	}


	@Override
	public String toString() {
		return String.format("%s %s:%s=%s", m_timestamp, m_resource.getIdentifier(), m_metric.getName(), m_value);
	}


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_metric == null) ? 0 : m_metric.hashCode());
        result = prime * result + ((m_resource == null) ? 0 : m_resource.hashCode());
        result = prime * result + ((m_timestamp == null) ? 0 : m_timestamp.hashCode());
        result = prime * result + ((m_value == null) ? 0 : m_value.hashCode());
        return result;
    }


    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Sample)) {
            return false;
        }
        final Sample other = (Sample) obj;
        if (m_metric == null) {
            if (other.m_metric != null) {
                return false;
            }
        } else if (!m_metric.equals(other.m_metric)) {
            return false;
        }
        if (m_resource == null) {
            if (other.m_resource != null) {
                return false;
            }
        } else if (!m_resource.equals(other.m_resource)) {
            return false;
        }
        if (m_timestamp == null) {
            if (other.m_timestamp != null) {
                return false;
            }
        } else if (!m_timestamp.equals(other.m_timestamp)) {
            return false;
        }
        if (m_value == null) {
            if (other.m_value != null) {
                return false;
            }
        } else if (!m_value.equals(other.m_value)) {
            return false;
        }
        return true;
    }



}
