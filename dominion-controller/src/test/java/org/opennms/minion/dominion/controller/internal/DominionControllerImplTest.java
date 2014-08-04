package org.opennms.minion.dominion.controller.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opennms.minion.api.MinionException;
import org.opennms.minion.api.MinionInitializationMessage;
import org.opennms.minion.api.MinionMessage;
import org.opennms.minion.api.MinionMessageSender;
import org.opennms.minion.api.MinionStatusMessage;
import org.opennms.minion.api.StatusMessageWriter;

@RunWith(MockitoJUnitRunner.class)
public class DominionControllerImplTest {
    @Test
    public void testInit() throws MinionException {
        final DominionControllerImpl controller = new DominionControllerImpl();
        controller.m_camelContextInitialized = true;
        controller.setBrokerUri("foo");
        controller.setListenQueueName("initialization");
        controller.setStatusMessageWriter(mock(StatusMessageWriter.class));
        controller.start();
    }

    @Test
    public void testOnMessage() throws MinionException {
        final CamelContext camelContext = mock(CamelContext.class);
        final ProducerTemplate producer = mock(ProducerTemplate.class);
        when(camelContext.createProducerTemplate()).thenReturn(producer);
        final DominionControllerImpl controller = new DominionControllerImpl();
        controller.m_camelContextInitialized = true;
        controller.setCamelContext(camelContext);
        controller.setBrokerUri("foo");
        controller.setListenQueueName("initialization");

        final StatusMessageWriter statusMessageWriter = mock(StatusMessageWriter.class);
        controller.setStatusMessageWriter(statusMessageWriter);

        final MockMessageSender sender = new MockMessageSender();
        controller.setMessageSender(sender);

        controller.start();

        final Map<String, String> emptyMap = Collections.emptyMap();
        final MinionStatusMessage message = new MinionStatusMessage() {
            @Override public int                 getVersion()    { return 1; }
            @Override public String              getId()         { return "Foo"; }
            @Override public String              getLocation()   { return "Bar"; }
            @Override public String              getStatus()     { return "Started"; }
            @Override public Date                getDate()       { return new Date(0); }
            @Override public Map<String, String> getProperties() { return emptyMap; }
        };
        controller.onMessage(message);
        verify(statusMessageWriter, times(1)).write("Foo", "Bar", "Started", new Date(0), emptyMap);

        final List<MinionMessage> responses = sender.getMessages();
        assertEquals(1, responses.size());
        final MinionMessage mess = responses.get(0);
        assertNotNull(mess);
        assertTrue(mess instanceof MinionInitializationMessage);
        final MinionInitializationMessage initMess = (MinionInitializationMessage)mess;
        assertEquals(2, initMess.getFeatureRepositories().size());
    }

    private static final class MockMessageSender implements MinionMessageSender {
        private List<MinionMessage> m_messages = new ArrayList<MinionMessage>();

        @Override public void sendMessage(MinionMessage message) throws MinionException {
            m_messages.add(message);
        }

        public List<MinionMessage> getMessages() {
            return m_messages;
        }
    }
}