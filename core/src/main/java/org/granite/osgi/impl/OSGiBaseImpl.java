package org.granite.osgi.impl;


import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import org.granite.osgi.OSGiBase;

import java.util.LinkedList;
import java.util.List;

@Component
@Instantiate
@Provides
public class OSGiBaseImpl implements OSGiBase {

    private static List<Class> classes = new LinkedList<Class>();

    public static Class<?> forName(String type) throws ClassNotFoundException {
        for (Class clazz : classes) {
            try {
                return clazz.getClassLoader().loadClass(type);
            } catch (Exception ex) {

            }
        }
        return null;
    }

    public static <T> Class<T> forName(String type, Class<T> cast) throws
            ClassNotFoundException {
        for (Class clazz : classes) {
            try {
                return (Class<T>) clazz.getClassLoader().loadClass(
                        type);
            } catch (Exception ex) {

            }
        }
        return null;
    }

    public void registerClass(Class obj) {
        classes.add(obj);
    }

    public void unregisterClass(Class obj) {
        classes.remove(obj);
    }
}
