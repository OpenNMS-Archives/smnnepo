package org.opennms.netmgt.api.sample.support;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class will convert all incoming objects to URLs.
 */
public class UrlNormalizer {

    public URL toURL(String s) throws MalformedURLException {
        return new URL(s);
    }

    public URL toURL(URL u) {
        return u;
    }
}