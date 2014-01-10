package org.opennms.netmgt.sample.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.AgentRepository;
import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.api.sample.MetricRepository;
import org.opennms.netmgt.api.sample.Resource;
import org.opennms.netmgt.api.sample.Results;
import org.opennms.netmgt.api.sample.Results.Row;
import org.opennms.netmgt.api.sample.Sample;
import org.opennms.netmgt.api.sample.SampleProcessorBuilder;
import org.opennms.netmgt.api.sample.SampleRepository;
import org.opennms.netmgt.api.sample.Timestamp;
import org.opennms.netmgt.api.sample.math.Rate;
import org.opennms.netmgt.api.sample.math.RollUp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/samples") 
@Produces(MediaType.APPLICATION_JSON) 
public class SampleResource {
	private static final Logger	LOG	= LoggerFactory.getLogger(SampleResource.class);

	private SampleRepository m_sampleRepository;
	private AgentRepository<?> m_agentRepository;
	private MetricRepository m_metricRepository;

	@GET
	@Path("{agentId}/{resourceType}/{resourceName}/{metric}")
	public String getSamples(@PathParam("agentId") String agentId, @PathParam("resourceType") String resourceType,
			@PathParam("resourceName") String resourceName, @PathParam("metric") String metricNames,
			@QueryParam("start") String start, @QueryParam("end") String end) {

		Agent agent = m_agentRepository.getAgentById(agentId);

		// SNMP:127.0.0.1:161|ifIndex|wlan0-84:3a:4b:0e:89:94
		Resource r = new Resource(agent, resourceType, resourceName);
		
		/*
		 * FIXME: Start and end as integer seconds. This works, but "Java(tm)
		 * Sucks At Time Parsing" seems to be bad reason for not being more
		 * flexible here.
		 */
		Integer iStart = null, iEnd = null;
		try {
			if (start != null) {
				iStart = Integer.parseInt(start);
			}
			if (end != null) {
				iEnd = Integer.parseInt(end);
			}
		}
		catch (NumberFormatException e) {
			LOG.error("Unable to parse start and/or end timestamp as seconds");
			throw new WebApplicationException(Response.serverError().build());
		}

		Timestamp endTs = (iEnd != null) ? new Timestamp(iEnd, TimeUnit.SECONDS) : Timestamp.now();
		Timestamp startTs = (iStart != null) ? new Timestamp(iStart, TimeUnit.SECONDS) : endTs.minus(6, TimeUnit.MINUTES);

		/*
		 * FIXME: CSV metrics done here to get something working; Not an
		 * explicit design decision.
		 */
		// Matt made me do this!
		List<Metric> metrics = new ArrayList<Metric>();

		for (String metricName : metricNames.split(",")) {
			metrics.add(m_metricRepository.getMetric(metricName));
		}

		LOG.debug("Parsed {} metrics from request", metrics.size());
		LOG.debug("Processing metrics as {}", metrics.get(0).getType());

		/*
		 * FIXME: Validate the list of metrics. Make sure there is at least one
		 * not-null (they can be null), and that they are all of the same type.
		 * Consider changing the JSON output so that errors can be passed back
		 * to the client instead of excepting here.
		 */

		SampleProcessorBuilder bldr = new SampleProcessorBuilder();

		switch (metrics.get(0).getType()) {
			case COUNTER:
				bldr.append(new Rate());
				break;
			case ABSOLUTE:
				// TODO: do
				break;
			case DERIVE:
				// TODO: do
				break;
			case GAUGE:
				break;
		}

		bldr.append(new RollUp(200, 300, TimeUnit.SECONDS));

		long tstamp = System.currentTimeMillis();

		Results results = m_sampleRepository.find(bldr, startTs, endTs, r, metrics.toArray(new Metric[0]));

		LOG.debug("Retrieved results from SampleRepository in {} msecs", (System.currentTimeMillis()-tstamp));

		StringBuilder sb = new StringBuilder();
		boolean first = false;

		sb.append('[').append("\n");

		for (Row row : results) {
			for (Metric met : results.getMetrics()) {
				if (!first) {
					sb.append("  [");
					first = true;
				}
				else {
					sb.append(",\n [");
				}
				
				Sample sample = row.getSample(met);
				sb.append('"').append(met.getName()).append('"').append(',');
				sb.append(sample.getTimestamp().asMillis()).append(',');
				double value = sample.getValue().doubleValue();
				if (Double.isNaN(value) || Double.isInfinite(value)) {
					sb.append("null");
				} else {
					sb.append(value);
				}


				sb.append("]");
			}
		}

		sb.append(']');

		LOG.debug("Returning JSON results for {} sample rows", results.getRows().size());

		return sb.toString();
	}

	public void setSampleRepository(SampleRepository sampleRepo) {
		m_sampleRepository = sampleRepo;
	}

	public void setAgentRepository(AgentRepository<?> agentRepository) {
		m_agentRepository = agentRepository;
	}

	public void setMetricRepository(MetricRepository metricRepository) {
		m_metricRepository = metricRepository;
	}
}
