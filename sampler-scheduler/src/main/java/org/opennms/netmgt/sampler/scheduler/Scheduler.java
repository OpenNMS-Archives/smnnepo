package org.opennms.netmgt.sampler.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Consume;
import org.apache.camel.Produce;
import org.opennms.netmgt.api.sample.PackageAgentList;
import org.opennms.netmgt.api.sample.ServiceAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scheduler {
    private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);
    private final ScheduledExecutorService m_executor;
    private String m_scheduleEndpoint;
    private String m_dispatcherEndpoint;

    @Produce(property="dispatcherEndpoint")
    Dispatcher m_dispatcher;

    public Scheduler(int poolSize) {
        m_executor = Executors.newScheduledThreadPool(poolSize);
    }

    public Dispatcher getDispatcher() {
        return m_dispatcher;
    }
    public void setDispatcher(final Dispatcher dispatcher) {
        m_dispatcher = dispatcher;
    }

    public String getDispatcherEndpoint() {
        return m_dispatcherEndpoint;
    }
    public void setDispatcherEndpoint(final String uri) {
        m_dispatcherEndpoint = uri;
    }

    public String getScheduleEndpoint() {
        return m_scheduleEndpoint;
    }
    public void setScheduleEndpoint(final String uri) {
        m_scheduleEndpoint = uri;
    }

    @Consume(property="scheduleEndpoint")
    public void onAgentSchedule(final PackageAgentList agentSchedule) {
        LOG.debug("Scheduling agents: {}", agentSchedule);
        final Long interval = agentSchedule.getInterval();
        final String service = agentSchedule.getServiceName();

        int count = 0;
        double offset = interval / (double)agentSchedule.getAgents().size();

        for (final ServiceAgent agent : agentSchedule.getAgents()) {
            final CollectionRequest request = new CollectionRequest(service, agent);
            m_executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    m_dispatcher.dispatch(request);
                }
            }, (long)(count * offset), interval, TimeUnit.MILLISECONDS);
            count++;
        }
    }

}
