package org.granite.osgi;

public interface GraniteClassRegistry {

    public void registerClass(String destination, Class clazz);
    public void unregisterClass(String destination, Class clazz);

    public Class<?> forName(String destination, String type) throws ClassNotFoundException;
}
