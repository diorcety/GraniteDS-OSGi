package org.granite.osgi;

public interface GraniteClassRegistry {

    public void registerClass(Class<?> clazz);
    public void unregisterClass(Class<?> clazz);
}
