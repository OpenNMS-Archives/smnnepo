package org.opennms.netmgt.api.sample;

public class SampleProcessorBuilder {
	private SampleProcessor m_entrance;
	private SampleProcessor m_exit;
	
	public SampleProcessorBuilder() {}
	
	public SampleProcessorBuilder append(SampleProcessor processor) {
		processor.setProducer(m_exit);
		m_exit = processor;
		if (m_entrance == null) {
			m_entrance = m_exit;
		}
		return this;
	}

	public SampleProcessorBuilder prepend(SampleProcessor processor) {
		if (m_entrance != null) {
			m_entrance.setProducer(processor);
		}
		m_entrance = processor;
		if (m_exit == null) {
			m_exit = m_entrance;
		}
		return this;
	}

	public SampleProcessor getProcessor() {
		return m_exit;
	}

	public String toString() {
		return String.format("%s(entrance=%s, exit=%s)", getClass().getSimpleName(), m_entrance, m_exit);
	}
}
