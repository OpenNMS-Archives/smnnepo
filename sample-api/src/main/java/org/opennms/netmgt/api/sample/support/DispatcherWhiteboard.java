package org.opennms.netmgt.api.sample.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This class will convert all incoming objects to URLs.
 */
public class DispatcherWhiteboard {

	private BundleContext context;
	private Class<?> messageClass;
	private Class<?> serviceClass;
	private String methodName = "dispatch";

	private ServiceTracker tracker = null;
	private Method method = null;

	public DispatcherWhiteboard() {}

	public void setContext(BundleContext context) {
		this.context = context;
	}

	public Class<?> getMessageClass() {
		return messageClass;
	}

	public void setMessageClass(Class<?> messageClass) {
		this.messageClass = messageClass;
	}

	public Class<?> getServiceClass() {
		return serviceClass;
	}

	public void setServiceClass(Class<?> serviceClass) {
		this.serviceClass = serviceClass;
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

	public void dispatch(Object message) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (tracker == null) {
			tracker = new ServiceTracker(context, serviceClass, null);
			tracker.open();
		}

		if (method == null) {
			method = serviceClass.getMethod(methodName, messageClass);
		}

		for (Object service : tracker.getServices()) {
			method.invoke(service, message);
		}
	}
}
