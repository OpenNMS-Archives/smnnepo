package org.opennms.netmgt.sampler.config.snmp;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class Parser {
    private static Logger LOG = LoggerFactory.getLogger(Parser.class); 
    private final Unmarshaller m_unmarshaller;
    private String m_username;
    private String m_password;

    public Parser() throws JAXBException {
        m_unmarshaller = JAXBContext.newInstance(DataCollectionConfig.class, DataCollectionGroup.class).createUnmarshaller();
    }

    public Parser(final String username, final String password) throws JAXBException {
        this();
        m_username = username;
        m_password = password;
    }

    public DataCollectionConfig getDataCollectionConfig(final URL path) throws JAXBException, IOException {
        return parse(path, DataCollectionConfig.class);
    }

    public DataCollectionGroup getDataCollectionGroup(final URL path) throws JAXBException, IOException {
        return parse(path, DataCollectionGroup.class);
    }

    private <T> T parse(URL url, Class<T> declaredType) throws JAXBException, IOException {
        InputStream urlStream = null;
        final URLConnection connection = url.openConnection();

        try {
            LOG.debug("Unmarshalling {} as {}", url, declaredType);

            String username = m_username;
            String password = m_password;

            final String userInfo = url.getUserInfo();
            if (userInfo != null && userInfo.contains(":")) {
                final String[] auth = userInfo.split(":", 2);
                username = auth[0];
                password = auth[1];
            }

            final String basicAuth = "Basic " + new String(new Base64().encode((username + ":" + password).getBytes()));
            connection.setRequestProperty("Authorization", basicAuth);
            connection.connect();
            urlStream = connection.getInputStream();
            JAXBElement<T> jaxbElement = m_unmarshaller.unmarshal(new StreamSource(urlStream), declaredType);
            return jaxbElement == null ? null : jaxbElement.getValue();		
        } catch (IOException e) {
            LOG.warn("Could not unmarshal " + url.toString() + " as " + declaredType.getName(), e);
            return null;
        } finally {
            if (urlStream != null) {
                urlStream.close();
            }
        }
    }
}