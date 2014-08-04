package org.opennms.minion.dominion.controller.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opennms.minion.api.MinionException;
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
        verify(producer, times(1)).asyncRequestBody(anyString(), any());
        verify(statusMessageWriter, times(1)).write("Foo", "Bar", "Started", new Date(0), emptyMap);
    }
}
