package org.opennms.netmgt.sampler.config.snmp;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="expression")
@XmlAccessorType(XmlAccessType.FIELD)
public class Expression {

	@XmlElement(name="template")
	public String template;

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

}
