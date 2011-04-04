package org.granite.osgi;

public interface OSGiBase {
    public void registerClass(Class obj);

    public void unregisterClass(Class obj);
}
