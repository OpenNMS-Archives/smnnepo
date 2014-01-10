package org.opennms.netmgt.api.sample;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

import java.io.File;
import java.net.InetAddress;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.opennms.netmgt.api.sample.Results.Row;
import org.opennms.netmgt.api.sample.support.SimpleFileRepository;

/**
=== TODOs ===

=== Collectors ===
- Need to define different (pluggable) ways to collect the data.
- Also need to allow for setting data from other pars of the system, for example, availability data

=== Model ===
- Querying resources by type|name|attribute?
- Nest resources (cpu lives on a node etc)
- Metric must be of different types (Counter, Gauge, Int, String)

==== Config ====
- Define a resource
- Define the source of data for a resource
- Define a metric to be collected
- Define resource properties to be collected and stored
- Define data roll-up schedule

==== Collect ====
- Collect data for a metric
- Deliver collected data to persister
- Retrieve data for a metric
- Threshold collected data
- Send data to realtime display

==== Graph ====
- Define a graph
- Determine metrics required to make a graph
- Determine if a resource supports the metrics for a graph
- Generate a graph for a resource
- 'register' to receive collected data

==== Scheduling ====
- Schedule metrics to be collected for resources
- Querying resources? by next scheduled time

=== Thoughts ===

System to says 'collect this'
Collectors should just collect and say 'here's the results'
Schedulers should just 'its' time!'
Persistance should just get 'store this'


*/

public class SampleTest 
{
	protected SampleRepository m_repository;
	JvmCollector m_collector;

	// for each collector
	// - what KIND of resources exist
	// - what actual resources do i know about
	// - ask what can be collected for a give kind of resource (for this collector)
	// - decide what I want to collect and which resources do i want to collect it for
	// - tell the collector to collect it
	
	
	// graph data for non-nodes
	// switch away from RRD based backend
	// distribute data collection across multiples nodes
	// switch graphing technologies to something other rrd
	// graph data in real time
	// current scheduling requires huge query
	// configuration is a PITX
	//
	// Thoughts:
	// - saving collected data should require no reading from the database (this is why rrd is slow)
	// - each subsystem should be 'pluggable' to prevent lock in like we currently have
	// - use SOA for interaction between plugins so they can come and go at runtime without impacting each other
	// - scheduling should be handled as a separate plugin that 'calls into' the collector with a description of data to be collected
	// - 
	
	// Subsystems/Components:
	// - Collector
	// - Datasource (for example Poller, Sniffer, Other instrumented processes)
	// - Persister
	// - Repository
	// - Thresholder
	// - Scheduler
	// - Grapher
	// - Configuration
	// (Collect what?)
	// (Collect it from who?)
	// 
	// These things are 'collector/protocol type dependent
	// Collector
	// Agent (collect from an agent)
	// Resource (collect data about this)
	// Metric (data to collect)
	// Measurement (the result of collecting)
	//
	// Ideas:
	// We could have an interface that represents some kind of 'metric provider'
	// and a collector 'bundle' would register a provider that enumerated the 'available' resources and metrics
	// known (or configured) for that kind of collector.  Implies, agent types, and implies resource types.
	//
	// Only the SNMP collector knows how to get data from an SNMP agent for an SNMP resource/metric
	//
	// Configuration options? collect data for all resources found vs collect data only for known/configured resources
	//
	// Next steps:
	// 1. Switch to using SOA for registration of observers, persisters, repositories, etc.
	// 2. Define Configuration 'metric provider' api?
	// 3. Define Scheduling mechanism
	// 4. Write LegagcyRepository that matches the current file system structure
	// 5. Write a JRobinGrapher that uses graph data from LegacyRepository
	// 6. Write a PostgresRepository
	// 7. Write a CassandraRepository
	// 8. Enhance the JRobinGrapher to graph from non RRD repositories
	// 9. Create Thresholder
	// 
	//
	// 
	
	// 
	//
	public static class Graph{
		public static class Canvas{
			char[][] m_pixels;
			int m_width;
			int m_height;
			
			public Canvas(int width, int height) {
				m_pixels = new char[height][width];
				for(int i = 0; i < m_pixels.length; i++) {
					for(int j = 0; j < m_pixels[i].length; j++) {
						m_pixels[i][j] = ' ';
					}
				}
				m_width = width;
				m_height = height;
			}
			
			public int getWidth() {
				return m_width;
			}
			public int getHeight() {
				return m_height;
			}
			public void plot(int xPixel, int yPixel) {
				System.err.println("adding pixel x: " + xPixel + " y: " + yPixel);
				if(yPixel < getHeight() && xPixel < getWidth() && yPixel >=0 && xPixel >=0) {
					m_pixels[yPixel][xPixel] = '*';
				}
			}
			
			public void print() {
				
				for(int y = m_height -1; y >=0; y--) {
					for(int x = 0; x < m_width; x++) {
						System.err.print(m_pixels[y][x]);
					}
					System.err.println();
				}
			}
			
			
		}
		
		public Graph(Results results) {
			Metric metric = results.getMetrics().get(0);
			SortedSet<Sample> column = results.getColumn(metric);

			double yMin = getMinValue(column);
			double yMax = getMaxValue(column);
			double yPixelRange = (yMax - yMin) / (m_canvas.getHeight() -1);
			
			Timestamp xMin = column.first().getTimestamp();
			Timestamp xMax = column.last().getTimestamp();
			long xPixelRange = Math.max(1, (xMax.asSeconds() - xMin.asSeconds()) / (m_canvas.getWidth() -1));
			
			for(Sample measurement : column) {
				Timestamp ts = measurement.getTimestamp();
				SampleValue<?> val = measurement.getValue();
				int xPixel = (int) ((ts.asSeconds() - xMin.asSeconds()) / xPixelRange);
				int yPixel = val.subtract(yMin).divide(yPixelRange).intValue();
				
				m_canvas.plot(xPixel, yPixel);
			}
		}
		
		public void draw() {
			m_canvas.print();
			
		}
		
		private double getMaxValue(SortedSet<Sample> column) {
			double max = Double.MIN_VALUE;
			for(Sample m : column) {
				max = Math.max(m.getValue().doubleValue(), max);
			}
			return max;
		}

		private double getMinValue(SortedSet<Sample> column) {
			double min = Double.MAX_VALUE;
			for(Sample m : column) {
				min = Math.min(m.getValue().doubleValue(), min);
			}
			return min;
		}

		/* 
		 * Timestamp horizontal
		 * Value vertical
		 *
		 */
		
		Canvas m_canvas = new Canvas(80, 25);
		
	}
	
	public class Grapher {
		
		public Grapher() {}
		
		public Graph createGraph(Results results) {
			return new Graph(results);
		}
	}
	
	@Before
	public void setUp() throws Exception {

		registerRepository();
		
		createCollectors();
		
	}

	private void createCollectors() throws Exception {
		m_collector = new JvmCollector();
		
	}

	protected void registerRepository() {
		// register repository
		m_repository = new SimpleFileRepository(new File("/tmp/attributes.txt"), new File("/tmp/samples.txt"));
	}
	
	@Test
    public void testCollect() throws Exception
    {
		assumeThat(m_repository, notNullValue());

		SampleRepository repository = m_repository;

		JvmCollector collector = m_collector;
		
		int numMeasurements = 0;

		Timestamp start = Timestamp.now();
		for(int i = 0; i < 10; i++) {
			SampleSet s = collector.collect();
			m_repository.save(s);
			numMeasurements++;

			System.err.print("--------------------");
			if ((numMeasurements % 10) == 0) System.err.print(numMeasurements);
			System.err.println();

			Thread.sleep(1000);
		}
		Timestamp end = Timestamp.now();

		System.err.printf("%nCollection complete; %d measurements captured%n", numMeasurements);


		// this should be registered as a service
		Grapher grapher = new Grapher();

		// the below code belongs in the grapher
		// location resource and metric 'provider' to look these up
		Resource resource = collector.getResource(
				String.format("http:%s:80|jvm|resources", InetAddress.getLocalHost().getHostAddress()));
		Metric[] searchMetrics = new Metric[] {
				//collector.getMetric("availableProcessors"),
				collector.getMetric("freeMemory"),

		};
		
		start = start.atStepBoundaryStart(1000, TimeUnit.MILLISECONDS);
		end = end.atStepBoundaryStart(1000, TimeUnit.MILLISECONDS);
		System.err.println("start: " + start);
		System.err.println("end: " + end);
		resource.getAttributes().clear();

		// Ensure attributes are cleared
		assertNull(resource.getAttribute("version"));
		assertNull(resource.getAttribute("vendor"));

		// TODO this needs to return something
		Results results = repository.find(null, start, end, resource, searchMetrics);

		// Ensure find() populated resource with attributes
		assertEquals("6", results.getResource().getAttribute("version"));
		assertEquals("openjdk", results.getResource().getAttribute("vendor"));


		// This would be a 'report' graph
		System.err.println("------------- RESULTS ---------------");
		System.err.printf("%-30s ", "Timestamp");
		for(Metric metric : results.getMetrics()) {
			System.err.printf("%20s ", metric.getName());
		}
		System.err.println();
		for(Row r : results.getRows()) {
			System.err.printf("%-30s ", r.getTimestamp());
			for(Metric metric : results.getMetrics()) {
				System.err.printf("%20.2f ", r.getSample(metric).getValue().doubleValue());
			}
			System.err.println();
		}
		
		// Validate that search returned the expected number of results.
		int numResults = results.getColumn(collector.getMetric("freeMemory")).size();
		assertTrue(String.format("Unexpected number of elements received expected %d, reeived %d", numMeasurements, numResults), numResults - numMeasurements < 2);

		// this creates a 'ascii' graph
		Graph graph = grapher.createGraph(results); // need to define a way to indicate 'WHICH' graph
		graph.draw();
		
    }		
		
	
}
