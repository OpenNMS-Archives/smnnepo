package org.opennms.netmgt.sampler.scheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.PackageAgentList;
import org.opennms.netmgt.api.sample.scheduler.CollectionRequest;
import org.opennms.netmgt.api.sample.support.Dispatcher;
import org.opennms.netmgt.api.sample.support.SchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scheduler implements SchedulerService {
    private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);
    private static final Dispatcher NULL_DISPATCHER = new Dispatcher() {
        @Override public void dispatch(final CollectionRequest request) {
            LOG.debug("No dispatcher for scheduled request {}", request);
        }
    };
    private final ScheduledExecutorService m_executor;

    final ConcurrentHashMap<String,Dispatcher> m_dispatchers = new ConcurrentHashMap<String, Dispatcher>();

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

        int count = 0;
        double offset = interval / (double)agentSchedule.getAgents().size();

        for (final Agent agent : agentSchedule.getAgents()) {
            final CollectionRequest request = new CollectionRequest(service, agent);
            m_executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    getDispatcher(service).dispatch(request);
                }
            }, (long)(count * offset), interval, TimeUnit.MILLISECONDS);
            count++;
        }
    }
}