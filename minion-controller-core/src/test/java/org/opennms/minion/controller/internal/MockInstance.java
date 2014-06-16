package org.opennms.minion.controller.internal;

import org.apache.karaf.admin.Instance;

final class MockInstance implements Instance {
    private String m_name;
    private String m_state = Instance.STARTED;

    public MockInstance(String name) {
        m_name = name;
    }

    @Override
    public String getName() {
        return m_name;
    }

    @Override
    public void setName(final String name) {
        m_name = name;
    }

    @Override
    public boolean isRoot() {
        return ("root".equalsIgnoreCase(m_name));
    }

    @Override
    public String getLocation() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public void setLocation(String location) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public int getPid() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public int getSshPort() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public void changeSshPort(int port) throws Exception {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public int getRmiRegistryPort() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public void changeRmiRegistryPort(int port) throws Exception {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public int getRmiServerPort() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public void changeRmiServerPort(int port) throws Exception {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public String getJavaOpts() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public void changeJavaOpts(String javaOpts) throws Exception {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public void start(String javaOpts) throws Exception {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public void stop() throws Exception {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public void destroy() throws Exception {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public String getState() throws Exception {
        return m_state;
    }

    public void setState(final String state) {
        m_state = state;
    }

    @Override
    public boolean isAttached() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }
    
}