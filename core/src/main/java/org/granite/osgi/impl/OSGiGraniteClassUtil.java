/*
  GRANITE DATA SERVICES
  Copyright (C) 2011 GRANITE DATA SERVICES S.A.S.

  This file is part of Granite Data Services.

  Granite Data Services is free software; you can redistribute it and/or modify
  it under the terms of the GNU Library General Public License as published by
  the Free Software Foundation; either version 2 of the License, or (at your
  option) any later version.

  Granite Data Services is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Library General Public License
  for more details.

  You should have received a copy of the GNU Library General Public License
  along with this library; if not, see <http://www.gnu.org/licenses/>.
*/

package org.granite.osgi.impl;

import org.apache.felix.ipojo.annotations.*;

import org.granite.logging.Logger;
import org.granite.osgi.GraniteClassRegistry;
import org.granite.osgi.service.GraniteDestination;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;

@Component
@Provides
@Instantiate
public class OSGiGraniteClassUtil implements GraniteClassRegistry, OSGiGraniteClassLoader {

    private static final Logger log = Logger.getLogger(OSGiGraniteClassUtil.class);

    private static ThreadLocal<String> destination_instance = new ThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return (null);
        }
    };

    public static void setDestination(String destination) {
        destination_instance.set(destination);
    }


    private Map<String, Map<String, Class>> destinationClasses = new Hashtable<String, Map<String, Class>>();


    @Validate
    private void starting() {
        log.debug("Start OSGiGraniteClassUtil");
    }

    @Invalidate
    private void stopping() {
        log.debug("Stop OSGiGraniteClassUtil");
    }

    public Class<?> forName(String type) throws ClassNotFoundException {
        if (destination_instance.get() != null) {
            Map<String, Class> classMap;
            synchronized (destinationClasses) {
                classMap = destinationClasses.get(destination_instance.get());
            }
            if (classMap != null && classMap.containsKey(type))
                return classMap.get(type);
        }
        throw new ClassNotFoundException(type);
    }

    public final void registerClass(String destination, Class clazz) {
        addClass(destination, clazz);
    }

    public final void unregisterClass(String destination, Class clazz) {
        removeClass(destination, clazz);
    }

    @Bind(aggregate = true, optional = true)
    public final void bindDestination(final GraniteDestination destination) {
        List<Class> list = analyseClass(destination.getClass());
        for (Class clazz : list) {
            addClass(destination.getId(), clazz);
        }
    }

    @Unbind
    public final void unbindDestination(final GraniteDestination destination) {
        List<Class> list = analyseClass(destination.getClass());
        for (Class clazz : list) {
            removeClass(destination.getId(), clazz);
        }
    }

    private void addClass(String destination, Class clazz) {
        synchronized (destinationClasses) {
            Map<String, Class> classes = destinationClasses.get(destination);
            if (classes == null) {
                classes = new Hashtable<String, Class>();
                destinationClasses.put(destination, classes);
            }

            classes.put(clazz.getName(), clazz);
        }
        log.info("Register class: " + clazz.getName() + " to " + destination);
    }

    private void removeClass(String destination, Class clazz) {
        synchronized (destinationClasses) {
            Map<String, Class> classes = destinationClasses.get(destination);
            if (classes != null) {
                classes.remove(clazz.getName());
                if (classes.size() == 0)
                    destinationClasses.remove(destination);
            }
        }
        log.info("Unregister class: " + clazz.getName() + " to " + destination);
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
