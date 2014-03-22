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
import org.opennms.netmgt.api.sample.Dispatcher;
import org.opennms.netmgt.api.sample.PackageAgentList;
import org.opennms.netmgt.api.sample.support.SchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scheduler implements SchedulerService {
    private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);
    private static final Dispatcher NULL_DISPATCHER = new Dispatcher() {
        @Override public void dispatch(final Agent agent) {
            LOG.debug("No dispatcher for scheduled request {}", agent);
        }
    };
    private final ScheduledExecutorService m_executor;

    private final ConcurrentHashMap<String,Dispatcher> m_dispatchers = new ConcurrentHashMap<String, Dispatcher>();
    private final ConcurrentHashMap<String,List<ScheduledFuture<?>>> m_schedules = new ConcurrentHashMap<String,List<ScheduledFuture<?>>>();

    public Scheduler(int poolSize) {
        m_executor = Executors.newScheduledThreadPool(poolSize);
    }

    public Dispatcher getDispatcher(final String service) {
        final Dispatcher dispatcher = m_dispatchers.get(service);
        if (dispatcher == null) {
            return NULL_DISPATCHER;
        }
        return dispatcher;
    }

    public void setDispatcher(final String service, final Dispatcher dispatcher) {
        m_dispatchers.put(service, dispatcher);
    }

    public void removeDispatcher(final String service, final Dispatcher dispatcher) {
        m_dispatchers.remove(service, dispatcher);
    }

    public void onDispatcherBind(final Dispatcher dispatcher, @SuppressWarnings("rawtypes") final Map properties) {
        LOG.debug("binding {}: {}", dispatcher, properties);
        final String service = (String)properties.get("org.opennms.netmgt.sampler.scheduler.serviceName");
        if (service != null && dispatcher != null) {
            setDispatcher(service, dispatcher);
        }
    }

    public void onDispatcherUnbind(final Dispatcher dispatcher, @SuppressWarnings("rawtypes") final Map properties) {
        LOG.debug("unbinding {}: {}", dispatcher, properties);
        final String service = (String)properties.get("org.opennms.netmgt.sampler.scheduler.serviceName");
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
                    getDispatcher(service).dispatch(agent);
                }
            }, (long)(count * offset), interval, TimeUnit.MILLISECONDS);
            count++;
            futures.add(future);
        }
        
        m_schedules.put(id, futures);
    }
}
