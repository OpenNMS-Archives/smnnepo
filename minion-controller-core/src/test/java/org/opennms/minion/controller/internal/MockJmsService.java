package org.opennms.minion.controller.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.jms.JmsMessage;
import org.apache.karaf.jms.JmsService;

public class MockJmsService implements JmsService {
    private Map<String,String> m_factories = new HashMap<String,String>();
    private Set<String> m_queues = new LinkedHashSet<String>();
    private List<String> m_messages = new ArrayList<String>();

    @Override
    public List<String> connectionFactories() throws Exception {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public List<String> connectionFactoryFileNames() throws Exception {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public void create(String name, String type, String url) throws Exception {
        if (!"activemq".equals(type)) {
            throw new IllegalArgumentException("Only activemq is supported!");
        }
        m_factories.put(name, url);
    }

    @Override
    public void delete(String name) throws Exception {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public Map<String, String> info(String connectionFactory, String username, String password) throws Exception {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public int count(String connectionFactory, String queue, String username, String password) throws Exception {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public List<String> queues(String connectionFactory, String username, String password) throws Exception {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public List<String> topics(String connectionFactory, String username, String password) throws Exception {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public List<JmsMessage> browse(String connectionFactory, String queue, String selector, String username, String password) throws Exception {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public void send(String connectionFactory, String queue, String body, String replyTo, String username, String password) throws Exception {
        if (!m_factories.containsKey(connectionFactory)) {
            throw new IllegalArgumentException("Connection factory " + connectionFactory + " does not exist!");
        }
        m_queues.add(queue);
        System.err.println("Sending to " + queue + ": " + body);
        m_messages.add(body);
    }

    @Override
    public int consume(String connectionFactory, String queue, String selector, String username, String password) throws Exception {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public int move(String connectionFactory, String sourceQueue, String targetQueue, String selector, String username, String password) throws Exception {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    public int getMessageCount() {
        return m_messages.size();
    }

    public List<String> getMessages() {
        return m_messages;
    }

}
