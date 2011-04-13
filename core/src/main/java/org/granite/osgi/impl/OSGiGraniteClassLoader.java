package org.granite.osgi.impl;

import org.apache.felix.ipojo.annotations.*;

import org.granite.context.IGraniteClassLoader;
import org.granite.logging.Logger;
import org.granite.osgi.GraniteClassRegistry;
import org.granite.osgi.service.GraniteDestination;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;

@Component
@Provides
@Instantiate
public class OSGiGraniteClassLoader implements GraniteClassRegistry, IGraniteClassLoader, IGraniteDestinationSetter {

    private static final Logger log = Logger.getLogger(OSGiGraniteClassLoader.class);

    private Map<String, Map<String, Class>> destinationClasses = new Hashtable<String, Map<String, Class>>();

    private String currentDestination;

    @Validate
    private void starting() {
        log.debug("Start OSGiGraniteClassLoader");
    }

    @Invalidate
    private void stopping() {
        log.debug("Stop OSGiGraniteClassLoader");
    }

    @Override
    public void setDestination(String destination) {
        currentDestination = destination;
    }

    public Class<?> forName(String type) throws ClassNotFoundException {
        if (currentDestination != null) {
            Map<String, Class> classMap = destinationClasses.get(currentDestination);
            if (classMap != null && classMap.containsKey(type))
                return classMap.get(type);
        }
        throw new ClassNotFoundException(type);
    }

    public final synchronized void registerClass(String destination, Class clazz) {
        addClass(destination, clazz);
    }

    public final synchronized void unregisterClass(String destination, Class clazz) {
        removeClass(destination, clazz);
    }

    @Bind(aggregate = true, optional = true)
    public final synchronized void bindDestination(final GraniteDestination destination) {
        List<Class> list = analyseClass(destination.getClass());
        for (Class clazz : list) {
            addClass(destination.getId(), clazz);
        }
    }

    @Unbind
    public final synchronized void unbindDestination(final GraniteDestination destination) {
        List<Class> list = analyseClass(destination.getClass());
        for (Class clazz : list) {
            removeClass(destination.getId(), clazz);
        }
    }

    private void addClass(String destination, Class clazz) {
        Map<String, Class> classes = destinationClasses.get(destination);
        if (classes == null) {
            classes = new Hashtable<String, Class>();
            destinationClasses.put(destination, classes);
        }
        classes.put(clazz.getName(), clazz);
        log.info("Register class: " + clazz.getName() + " to " + destination);
    }

    private void removeClass(String destination, Class clazz) {
        Map<String, Class> classes = destinationClasses.get(destination);
        if (classes != null) {
            classes.remove(clazz.getName());
            if (classes.size() == 0)
                destinationClasses.remove(destination);
            log.info("Unregister class: " + clazz.getName() + " to " + destination);
        }
    }

    private List<Class> analyseClass(Class cls) {
        List<Class> list = new LinkedList<Class>();

        // Get Methods
        for (Method method : cls.getMethods()) {
            if ((method.getModifiers() & Modifier.PUBLIC) != 0) {
                // Get Parameters
                for (Class mcls : method.getParameterTypes()) {
                    getClasses(list, mcls);
                }
                for (Type type : method.getGenericParameterTypes()) {
                    getClasses(list, type);
                }

                // Get Return Type
                getClasses(list, method.getReturnType());
                getClasses(list, method.getGenericReturnType());
            }
        }

        // Filters
        for (Iterator<Class> it = list.iterator(); it.hasNext();) {
            Class clazz = it.next();

            // Only class
            if (clazz.isInterface()) {
                it.remove();
                continue;
            }

            // Only serizalizable
            boolean serializable = false;
            for (Class<?> icls : clazz.getInterfaces()) {
                if (icls == Serializable.class) {
                    serializable = true;
                }
            }
            if (!serializable) {
                it.remove();
                continue;
            }
        }

        return list;
    }

    private void getClasses(List<Class> list, Class cls) {
        if (isUsefull(cls))
            list.add(cls);
    }

    private void getClasses(List<Class> list, Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Type[] parameters = pt.getActualTypeArguments();
            for (Type ptype : parameters) {
                if (ptype instanceof Class) {
                    Class clazz = (Class) ptype;
                    if (isUsefull(clazz)) {
                        list.add(clazz);
                    }
                }
            }

        }
    }

    private boolean isUsefull(Class cls) {
        ClassLoader clsloader = cls.getClassLoader();
        if (clsloader != null)
            return true;
        return false;
    }
}
