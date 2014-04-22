package org.opennms.netmgt.api.sample;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.custommonkey.xmlunit.Difference;
import org.junit.runners.Parameterized.Parameters;
import org.opennms.core.network.IPAddress;
import org.opennms.core.test.xml.XmlTestNoCastor;

public class SampleSetXmlTest extends XmlTestNoCastor<SampleSet> {

    public SampleSetXmlTest(SampleSet sampleObject, Object sampleXml) {
        super(sampleObject, sampleXml, null);
    }

    @Override
    protected boolean ignoreDifference(final Difference d) {
        final String controlValue = d.getControlNodeDetail().getValue();
        final String testValue = d.getTestNodeDetail().getValue();
        
        if ("number of element attributes".equals(d.getDescription())) {
            return true;
        } else if ("attribute name".equals(d.getDescription())) {
            return ("null".equals(controlValue) && "type".equals(testValue));
        }

        return super.ignoreDifference(d);
    }
    
    @Parameters
    public static Collection<Object[]> data() throws Exception {
        return Arrays.asList(new Object[][] {
                {
                    getSampleSetXml(),
                    "<sample-set>\n" + 
                    "   <timestamp time=\"1398182585227\" unit=\"MILLISECONDS\"/>\n" + 
                    "   <samples>\n" + 
                    "      <sample>\n" + 
                    "         <resource name=\"resource_name\" type=\"resource_type\">\n" + 
                    "            <agent>\n" + 
                    "               <address>127.0.0.1</address>\n" + 
                    "               <port>80</port>\n" + 
                    "               <serviceName>snmp</serviceName>\n" + 
                    "               <id>1</id>\n" + 
                    "               <parameters/>\n" + 
                    "            </agent>\n" + 
                    "            <attributes/>\n" + 
                    "         </resource>\n" + 
                    "         <metric name=\"metric\" metric-type=\"counter\" group=\"metrics\"/>\n" + 
                    "         <timestamp time=\"1398182585227\" unit=\"MILLISECONDS\"/>\n" + 
                    "         <sample-value value=\"1.0\">043ff0000000000000</sample-value>\n" + 
                    "      </sample>\n" + 
                    "   </samples>\n" + 
                    "</sample-set>"
                }
        });
    }

    private static SampleSet getSampleSetXml() throws UnknownHostException {
        final Timestamp time = new Timestamp(1398182585227l, TimeUnit.MILLISECONDS);
        final Resource resource = new Resource(new Agent(new IPAddress("127.0.0.1"), 80, "snmp", "1"), "resource_type", "resource_name");
        final Metric metric = new Metric("metric", MetricType.COUNTER, "metrics");
        final SampleSet samplesIn = new SampleSet(time);
        samplesIn.addMeasurement(resource, metric, new GaugeValue(1));
        return samplesIn;
    }
}
