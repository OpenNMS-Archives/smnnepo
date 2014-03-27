package org.opennms.netmgt.sampler.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.AgentDispatcher;
import org.opennms.netmgt.api.sample.PackageAgentList;
import org.opennms.netmgt.api.sample.support.SchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scheduler implements SchedulerService {
    private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);
    private static final AgentDispatcher NULL_DISPATCHER = new AgentDispatcher() {
        @Override
        public void dispatch(final Agent agent) {
            LOG.debug("No dispatcher for scheduled request {}", agent);
        }
    };
    private final ScheduledExecutorService m_executor;

    private final ConcurrentHashMap<String,AgentDispatcher> m_dispatchers = new ConcurrentHashMap<String, AgentDispatcher>();
    private final ConcurrentHashMap<String,List<ScheduledFuture<?>>> m_schedules = new ConcurrentHashMap<String,List<ScheduledFuture<?>>>();

    public Scheduler(int poolSize) {
        m_executor = Executors.newScheduledThreadPool(poolSize);
    }

    public AgentDispatcher getDispatcher(final String service) {
        final AgentDispatcher dispatcher = m_dispatchers.get(service);
        if (dispatcher == null) {
            return NULL_DISPATCHER;
        }
        return dispatcher;
    }

    public void setDispatcher(final String service, final AgentDispatcher dispatcher) {
        m_dispatchers.put(service, dispatcher);
    }

    public void removeDispatcher(final String service, final AgentDispatcher dispatcher) {
        m_dispatchers.remove(service, dispatcher);
    }

    public void onDispatcherBind(final AgentDispatcher dispatcher, @SuppressWarnings("rawtypes") final Map properties) {
        final String service = (String)properties.get("org.opennms.netmgt.sampler.scheduler.serviceName");
        LOG.debug("onDispatcherBind(service={})", service);
        if (service != null && dispatcher != null) {
            setDispatcher(service, dispatcher);
        }
    }

    public void onDispatcherUnbind(final AgentDispatcher dispatcher, @SuppressWarnings("rawtypes") final Map properties) {
        final String service = (String)properties.get("org.opennms.netmgt.sampler.scheduler.serviceName");
        LOG.debug("onDispatcherUnbind(service={})", service);
        if (service != null && dispatcher != null) {
            removeDispatcher(service, dispatcher);
        }
    }

    @Override
    public void schedule(final PackageAgentList agentSchedule) {
        LOG.debug("Scheduling agents: {}", agentSchedule);
        final Long interval = agentSchedule.getInterval();
        final String service = agentSchedule.getServiceName();
        final String id = agentSchedule.getId();

        int count = 0;
        double offset = interval / (double)agentSchedule.getAgents().size();

        if (m_schedules.containsKey(id)) {
            LOG.debug("Existing schedules found for {}: canceling.", id);
            for (final ScheduledFuture<?> future : m_schedules.get(id)) {
                LOG.debug("Canceling: {}", future);
                future.cancel(false);
            }
        }

        final List<ScheduledFuture<?>> futures = new ArrayList<ScheduledFuture<?>>();
        for (final Agent agent : agentSchedule.getAgents()) {
            final ScheduledFuture<?> future = m_executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    final AgentDispatcher dispatcher = getDispatcher(service);
                    LOG.trace("Dispatching agent {} to {}", dispatcher);
                    dispatcher.dispatch(agent);
                }
            }, (long)(count * offset), interval, TimeUnit.MILLISECONDS);
            count++;
            futures.add(future);
        }

        m_schedules.put(id, futures);
    }

    @Override
    public String toString() {
        return "Scheduler[ dispatchers=" + m_dispatchers + ", schedules=" + m_schedules + " ]";
    }
}
