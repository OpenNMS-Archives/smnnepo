package org.opennms.netmgt.sampler.config.snmp;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.api.sample.MetricType;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpValue;

/**
 * <column oid=".1.3.6.1.2.1.25.2.3.1.2" alias="hrStorageType"  type="string" display-hint="1x:" />
 * 
 * @author brozow
 *
 */
@XmlRootElement(name="column")
@XmlAccessorType(XmlAccessType.FIELD)
public class Column {
	
	@XmlAttribute(name="oid")
	@XmlJavaTypeAdapter(SnmpObjIdXmlAdapter.class)
	private SnmpObjId m_oid;
	
	@XmlAttribute(name="alias")
	private String m_alias;
	
	@XmlAttribute(name="type")
	private String m_type;
	
	@XmlAttribute(name="display-hint")
	private String m_displayHint;

	public SnmpObjId getOid() {
		return m_oid;
	}

	public void setOid(SnmpObjId oid) {
		m_oid = oid;
	}
	
	public String getAlias() {
		return m_alias;
	}

	public void setAlias(String alias) {
		m_alias = alias;
	}

	public String getType() {
		return m_type;
	}

	public void setType(String type) {
		m_type = type;
	}
	
	public MetricType getMetricType() {
		String type = getType().toLowerCase();
		if (type.startsWith("counter")) {
			return MetricType.COUNTER;
		} else if (type.startsWith("gauge")) {
			return MetricType.GAUGE;
		} else if (type.startsWith("integer")) {
			return MetricType.GAUGE;
		} else {
			return null;
		}
	}

	public Metric createMetric(String groupName) {
		MetricType type = getMetricType();
		if (type == null) return null;
		return new Metric(getAlias(), type, groupName);
	}
	
	private static String formatInteger(String displayHint, long val) {
		if (displayHint.equals("x")) {
			return Long.toHexString(val);
		} else if (displayHint.equals("o")) {
			return Long.toOctalString(val);
		} else if (displayHint.equals("b")) {
			return Long.toBinaryString(val);
		} else if (displayHint.equals("d")) {
			return Long.toString(val);
		} else if (displayHint.startsWith("d")) {
			// must have a '-digits' part
			int moveLeft = Integer.decode(displayHint.substring(2));
			double result = val/(Math.pow(10, moveLeft));
			return String.format("%."+moveLeft+"f", result);
		} else {
			return Long.toString(val);
		}

	}
	
	private static class OctetFormatSpecifier {
		private static Charset UTF_8 = Charset.forName("UTF-8"); 
		private boolean m_repeatIndicator = false;
		private int m_octetCount = 2;
		// 'a' for ascii, 'd' for decimal, 'x' for hex, 'o' for octal
		private char m_displayFormat = 'a';
		private char m_displaySeparator = '\0';
		private char m_repeatTerminator = '\0';
		
		public boolean hasRepeatIndicator() {
			return m_repeatIndicator;
		}
		public void setRepeatIndicator(boolean repeatIndicator) {
			m_repeatIndicator = repeatIndicator;
		}
		public int getOctetCount() {
			return m_octetCount;
		}
		public void setOctetCount(int octetCount) {
			m_octetCount = octetCount;
		}
		public char getDisplayFormat() {
			return m_displayFormat;
		}
		public void setDisplayFormat(char displayFormat) {
			m_displayFormat = displayFormat;
		}
		public char getDisplaySeparator() {
			return m_displaySeparator;
		}
		public void setDisplaySeparator(char displaySeparator) {
			m_displaySeparator = displaySeparator;
		}
		public char getRepeatTerminator() {
			return m_repeatTerminator;
		}
		public void setRepeatTerminator(char repeatTerminator) {
			m_repeatTerminator = repeatTerminator;
		}
		
		public boolean hasDisplaySeparator() {
			return getDisplaySeparator() != '\0';
		}
		
		public boolean hasRepeatTerminator() {
			return getRepeatTerminator() != '\0';
		}
		
		public int apply(byte[] bytes, int index, StringBuilder buf) {
			int repeat = hasRepeatIndicator() ? bytes[index++] : 1;
		
			for(int i = 0; i < repeat; i++) {
				index = applyFormat(bytes, index, buf);
				if (index < bytes.length && hasDisplaySeparator() && (!hasRepeatTerminator() || i < repeat-1)) {
					buf.append(getDisplaySeparator());
				}
			}
			
			if (index < bytes.length && hasRepeatTerminator()) {
				buf.append(getRepeatTerminator());
			}
			
			return index;
		}
		private int applyFormat(byte[] bytes, int index, StringBuilder buf) {
			if (getDisplayFormat() == 'a') {
				for(int i = 0; i < getOctetCount() && index < bytes.length; i++) {
					buf.append((char)bytes[index++]);
				}
			} else if (getDisplayFormat() == 't') {
				int decodeLen = Math.min(getOctetCount(), bytes.length-index);
				byte[] decode = new byte[decodeLen];
				System.arraycopy(bytes, index, decode, 0, decodeLen);
				String result = new String(decode, UTF_8);
				buf.append(result);
				index += decodeLen;
			} else {
				long val = 0;
				for(int i = 0; i < getOctetCount() && index < bytes.length; i++) {
					val = (val << 8) + (bytes[index++] & 0xffL); 
				}
				if (getDisplayFormat() == 'd') {
					buf.append(Long.toString(val));
				} else if (getDisplayFormat() == 'x') {
					String hex =  Long.toHexString(val);
					// append leading zeros for hex
					for(int i = 0; i < (2*getOctetCount() - hex.length()); i++) {
						buf.append('0');
					}
					buf.append(hex);
				} else if (getDisplayFormat() == 'o') {
					buf.append(Long.toOctalString(val));
				}
			}
			return index;
		}
		
	}
	
	private static class OctetStringDisplayHintParser {
		private final String m_displayHint;
		private int m_current;
		private List<OctetFormatSpecifier> m_formatSpecifiers = new ArrayList<OctetFormatSpecifier>();
		
		public OctetStringDisplayHintParser(String displayHint) {
			m_displayHint = displayHint;
			m_current = 0;
		}
		
		public void parse() {
			while(hasMore()) {
				OctetFormatSpecifier specifier = new OctetFormatSpecifier();
				repeatIndicator(specifier);
				octetCount(specifier);
				displayFormat(specifier);
				if (displaySeparator(specifier)) {
					repeatTerminator(specifier);
				}
				add(specifier);
			}
			
		}
		
		public String apply(byte[] bytes) {
			
			int index = 0;
			int currentSpecifier = 0;
			OctetFormatSpecifier specifier = m_formatSpecifiers.get(currentSpecifier);
			StringBuilder buf = new StringBuilder();
			while(index < bytes.length) {
				index = specifier.apply(bytes, index, buf);
				currentSpecifier = Math.min(currentSpecifier+1, m_formatSpecifiers.size()-1);
				specifier = m_formatSpecifiers.get(currentSpecifier);
			}
			return buf.toString();
		}

		private boolean displaySeparator(OctetFormatSpecifier specifier) {

			if (hasMore() && !isDigit(peek()) && peek() != '*') {
				char displaySeparator = peek();
				match(displaySeparator);
				specifier.setDisplaySeparator(displaySeparator);
				
				return true;
			}
			 
			return false;

		}

		private void repeatTerminator(OctetFormatSpecifier specifier) {
			if (hasMore() && !isDigit(peek()) && peek() != '*') {
				char repeatTerminator = peek();
				match(repeatTerminator);
				specifier.setRepeatTerminator(repeatTerminator);
			}
		}

		private void displayFormat(OctetFormatSpecifier specifier) {
			if (in(peek(), "axodt")) {
				char displayFormat = peek();
				match(displayFormat);
				specifier.setDisplayFormat(displayFormat);  
			} else {
				throw new IllegalArgumentException("Expected display format char at index " + m_current + " of \"" + m_displayHint +"\" but found character " + peek());
			}
		}

		private void octetCount(OctetFormatSpecifier specifier) {
			if (isDigit(peek())) {
				int octetCount = 0;
				while(isDigit(peek())) {
					char digitChar = peek();
					match(digitChar);
					int digit = digitChar - '0';
					octetCount = octetCount*10 + digit;
				}
				specifier.setOctetCount(octetCount);
			} else {
				throw new IllegalArgumentException("Expected digit at index " + m_current + " of \"" + m_displayHint +"\" but found character " + peek());
			}
		}

		private void repeatIndicator(OctetFormatSpecifier specifier) {
			if (peek() == '*') {
				match('*');
				specifier.setRepeatIndicator(true);
			}
		}
		
		private void add(OctetFormatSpecifier specifier) {
			m_formatSpecifiers.add(specifier);
		}
		
		private boolean isDigit(char ch) {
			return in(ch, "0123456789");
		}
		
		private boolean in(char ch, String set) {
			return set.indexOf(ch) >= 0;
		}
		
		private void match(char matchChar) {
			if (!hasMore()) {
				throw new IllegalArgumentException("Unexpected End of String for display hint \"" + m_displayHint + "\"");
			}
			char c = m_displayHint.charAt(m_current);
			if (c != matchChar) {
				throw new IllegalArgumentException("Unexpected character '" + c + "' at index " + m_current + " of display hint \"" + m_displayHint + "\"");
			}
			m_current++;
		}
		
		private boolean hasMore() {
			return m_current < m_displayHint.length();
		}
		
		private char peek() {
			return m_displayHint.charAt(m_current);
		}
		
		
	}
	
	public static String formatOctetString(String displayHint, byte[] bytes) {
		OctetStringDisplayHintParser parser = new OctetStringDisplayHintParser(displayHint);
		parser.parse();
		return parser.apply(bytes);
	}

	public String getValue(SnmpValue val) {
		String displayHint = m_displayHint;
		if (displayHint == null) {
			return val.toDisplayString();
		} else if (val.getType() == SnmpValue.SNMP_OCTET_STRING) {
			return formatOctetString(m_displayHint, val.getBytes());
		} else if (val.getType() == SnmpValue.SNMP_INT32) {
			return formatInteger(m_displayHint, val.toLong());
		}
		return val.toDisplayString();
			
	}

}
