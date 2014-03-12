package org.opennms.netmgt.api.sample.support;

import java.lang.reflect.Method;

import org.apache.camel.Consume;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class will convert all incoming objects to URLs.
 */
public class DispatcherWhiteboard {
	
	private static final Logger LOG = LoggerFactory.getLogger(DispatcherWhiteboard.class);

	private BundleContext context;
	private Class<?> messageClass;
	private Class<?> serviceClass;
	private String methodName = "dispatch";
	private final String m_endpointUri;

	private ServiceTracker tracker = null;
	private Method method = null;

	public DispatcherWhiteboard(String endpointUri) {
		m_endpointUri = endpointUri;
	}

	public String getEndpointUri() {
		return m_endpointUri;
	}

	public void setContext(BundleContext context) {
		this.context = context;
	}

	public Class<?> getMessageClass() {
		return messageClass;
	}

	public void setMessageClass(Class<?> messageClass) {
		this.messageClass = messageClass;
	}

	public void setMessageClassAsString(String messageClass) throws ClassNotFoundException {
		this.messageClass = Class.forName(messageClass);
	}

	public Class<?> getServiceClass() {
		return serviceClass;
	}

	public void setServiceClass(Class<?> serviceClass) {
		this.serviceClass = serviceClass;
	}

	public void setServiceClassAsString(String serviceClass) throws ClassNotFoundException {
		this.serviceClass = Class.forName(serviceClass);
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public void destroy() {
		if (tracker != null) {
			tracker.close();
		}
	}

	@Consume(property="endpointUri")
	public void dispatch(Object message) throws NoSuchMethodException, SecurityException {
		if (tracker == null) {
			tracker = new ServiceTracker(context, serviceClass, null);
			tracker.open();
		}

		if (method == null) {
			method = serviceClass.getMethod(methodName, messageClass);
		}

		try {
			Object[] services = tracker.getServices();
			if (services != null && services.length > 0) {
				for (Object service : tracker.getServices()) {
					method.invoke(service, message);
				}
			}
		} catch (Throwable e) {
			// If anything goes wrong, log an error message
			// TODO: Use a dead-letter channel?
			LOG.warn("Message dispatch failed: " + e.getMessage(), e);
		}
	}
}
