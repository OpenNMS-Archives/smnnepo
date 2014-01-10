package org.opennms.netmgt.api.sample.support;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.opennms.netmgt.api.sample.GaugeValue;
import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.api.sample.NanValue;
import org.opennms.netmgt.api.sample.Resource;
import org.opennms.netmgt.api.sample.Results;
import org.opennms.netmgt.api.sample.Results.Row;
import org.opennms.netmgt.api.sample.Sample;
import org.opennms.netmgt.api.sample.SampleProcessorBuilder;
import org.opennms.netmgt.api.sample.SampleRepository;
import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.api.sample.Timestamp;

public class SimpleFileRepository implements SampleRepository {
	
	private static final String SEPERATOR = "$";
	
	private File m_attributesFile;
	private File m_sampleFile;
	
	public SimpleFileRepository(File attributesFile, File sampleFile) {
		m_attributesFile = attributesFile;
		m_sampleFile = sampleFile;
	}

    @Override
    public void save(SampleSet samples) {
    	PrintWriter out = null;
    	try {
    		out = new PrintWriter(new BufferedWriter(new FileWriter(m_sampleFile, true)));
    		
            for(Sample m : samples.getSamples()) {
                System.err.println(String.format(
                		"Saving Measurement: %s for %s at %s = %.1f",
                		m.getMetric().getName(),
                		m.getResource().getIdentifier(),
                		m.getTimestamp().asDate(),
                		m.getValue().doubleValue()));
                out.printf("%s %s %d %s\n", m.getResource().getIdentifier(), m.getMetric().getName(), m.getTimestamp().convert(TimeUnit.SECONDS), m.getValue());
            }

    		
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try { if (out != null) out.close(); } catch(Exception e) {}
		}
    	
    	Properties attributes = loadAttributes();     
    	
    	for(Resource r : samples.getResources()) {
    		for(Entry<String, String> attribute : r.getAttributes().entrySet()) {
    			attributes.setProperty(prefix(r)+attribute.getKey(), attribute.getValue());
    		}
    	}
    	
    	storeAttributes(attributes);
    }
    
    private String prefix(Resource r) {
    	return r.getIdentifier()+SEPERATOR;
    }
    
    private Properties loadAttributes() {
    	Properties attributes = new Properties() ;
    	if (!m_attributesFile.canRead()) {
    		return attributes;
    	}
    	
    	Reader r = null;
    	try {
    		r = new FileReader(m_attributesFile);
    		attributes.load(r);
    		return attributes;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try { if (r != null) r.close(); } catch(Exception e) {}
		}
    }
    
    private void storeAttributes(Properties attributes) {
    	Writer w = null;
    	try {
    		w = new FileWriter(m_attributesFile);
    		attributes.store(w, "Save attributes");
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try { if (w != null) w.close(); } catch(Exception e) {}
		}
    }
    
	@Override
	public Results find(SampleProcessorBuilder builder, Timestamp start, Timestamp end, Resource resource, Metric... metrics) {
		if (resource == null) throw new IllegalArgumentException("null resource argument");
		// TODO make this work
		if (builder != null) throw new IllegalArgumentException("builder functionality still needs to be implemented here!");
		
		loadAttributesForResource(resource);

		// Treat null start and/or end as an unbounded dimension.
		long startSeconds = (start == null) ? 0 : start.asSeconds();
		long endSeconds = (end ==  null) ? Long.MAX_VALUE : end.asSeconds();

		Scanner scanner = null;
		try {
			scanner = new Scanner(m_sampleFile);
			Results results = new Results(resource, metrics);
			
			while(scanner.hasNext()) {
				String resourceName = scanner.next();
				String metricName = scanner.next();
				long timeInSeconds = scanner.nextLong();
				GaugeValue value = new GaugeValue(scanner.nextDouble());

				if (resourceName.equals(resource.getIdentifier())
					&& startSeconds <= timeInSeconds
					&& timeInSeconds < endSeconds
				   ) 
				{

					for(Metric metric : metrics) {
						if (metricName.equals(metric.getName())) {
							Timestamp t = new Timestamp(timeInSeconds, TimeUnit.SECONDS);
							Sample m = new Sample(resource, metric, t, value);
							results.addSample(m);
						}
					}
				}
			}

			/*
			 * XXX: This back-fills any missing metrics with Double.NaN.
			 * Obviously it would be better not to do this in a second pass, but
			 * that calls for writing/parsing the files differently (samples
			 * from the same interval stored in one line), or for more
			 * cleverness than I can muster this very second.
			 * 
			 * This is currently only used for tests; Maybe it is Good Enough.
			 */
			for (Row row : results.getRows()) {
				for (Metric metric : metrics) {
					if (!row.containsSample(metric)) {
						row.addSample(new Sample(resource, metric, row.getTimestamp(), new NanValue()));
					}
				}
			}

			return results;
			
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} finally {
			if (scanner != null) scanner.close();
		}
	}

	private void loadAttributesForResource(Resource resource) {
		String resourcePrefix = prefix(resource);
		Properties attributes = loadAttributes();
		for(String key : attributes.stringPropertyNames()) {
			if (key.startsWith(resourcePrefix)) {
				String attrName = key.substring(resourcePrefix.length());
				String attrValue = attributes.getProperty(key);
				resource.setAttribute(attrName, attrValue);
			}
		}
	}
}