package org.opennms.minion.controller.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;
import org.opennms.minion.controller.internal.MinionStatusMessageImpl;

public class MinionStatusMessageTest {
    @Test
    public void testObjectToXml() throws Exception {
        final Date d = new Date(0);

        final MinionStatusMessageImpl message = new MinionStatusMessageImpl("12345", 1);
        message.setLocation("Here");
        message.setStatus("A-OK!");
        message.setDate(d);
        message.addProperty("thingy", "thing-like");

        final StringWriter writer = new StringWriter();
        final Marshaller marshaller = JAXBContext.newInstance(MinionStatusMessageImpl.class).createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(message, writer);
        final String xml = writer.toString();
        assertTrue(xml.contains("<minion-status "));
        assertTrue(xml.contains("id=\"12345\""));
        assertTrue(xml.contains("version=\"1\""));
        assertTrue(xml.contains("<location>Here</location>"));
        assertTrue(xml.contains("<status>A-OK!</status>"));
        assertTrue(xml.contains("<date>1970-01-01T00:00:00.000+0000</date>"));
        assertTrue(xml.contains("<properties>"));
        assertTrue(xml.contains("<property key=\"thingy\">thing-like</property>"));
    }
    
    @Test
    public void testXmlToObject() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
                "<minion-status id=\"12345\" version=\"1\">\n" + 
                "    <location>Here</location>\n" + 
                "    <status>A-OK!</status>\n" + 
                "    <date>1970-01-01T00:00:00.000+0000</date>\n" + 
                "    <properties>\n" + 
                "        <property key=\"thingy\">thing-like</property>\n" + 
                "    </properties>\n" + 
                "</minion-status>\n";
        
        final Unmarshaller unmarshaller = JAXBContext.newInstance(MinionStatusMessageImpl.class).createUnmarshaller();
        final StringReader reader = new StringReader(xml);
        final MinionStatusMessageImpl message = (MinionStatusMessageImpl)unmarshaller.unmarshal(reader);
        
        assertEquals("12345", message.getId());
        assertEquals("Here", message.getLocation());
        assertEquals("A-OK!", message.getStatus());
        assertEquals(new Date(0), message.getDate());
        assertEquals(1, message.getProperties().size());
        assertTrue(message.getProperties().containsKey("thingy"));
        assertEquals("thing-like", message.getProperties().get("thingy"));
        
        System.err.println(message);
    }
}
