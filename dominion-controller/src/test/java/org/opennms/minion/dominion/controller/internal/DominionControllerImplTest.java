package org.opennms.minion.dominion.controller.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opennms.minion.api.MinionException;
import org.opennms.minion.api.MinionStatusMessage;

@RunWith(MockitoJUnitRunner.class)
public class DominionControllerImplTest {

    @Test
    public void testInit() throws MinionException {
        final DominionControllerImpl controller = new DominionControllerImpl();
        controller.setBrokerUri("foo");
        controller.setListenQueueName("initialization");
        controller.start();
    }

    @Test
    public void testOnMessage() throws MinionException {
        final DominionControllerImpl controller = new DominionControllerImpl();
        controller.setBrokerUri("foo");
        controller.setListenQueueName("initialization");

        final StatusMessageWriter statusMessageWriter = mock(StatusMessageWriter.class);
        controller.setStatusMessageWriter(statusMessageWriter);

        controller.start();

        final Map<String, String> emptyMap = Collections.emptyMap();
        controller.onMessage(new MinionStatusMessage() {
            @Override public int                 getVersion()    { return 1; }
            @Override public String              getId()         { return "Foo"; }
            @Override public String              getLocation()   { return "Bar"; }
            @Override public String              getStatus()     { return "Started"; }
            @Override public Date                getDate()       { return new Date(0); }
            @Override public Map<String, String> getProperties() { return emptyMap; }
        });
        verify(statusMessageWriter, times(1)).write("Foo", "Bar", "Started", new Date(0), emptyMap);
    }
}
