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

    private static final Logger log = Logger.getLogger(
            OSGiGraniteClassUtil.class);

    private static ThreadLocal<String> destination_instance = new ThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return (null);
        }
    };

    public static void setDestination(String destination) {
        destination_instance.set(destination);
    }


    private Map<String, Class[]> destinationClasses = new Hashtable<String, Class[]>();


    @Validate
    private void starting() {
        log.debug("Start OSGiGraniteClassUtil");
    }

    @Invalidate
    private void stopping() {
        log.debug("Stop OSGiGraniteClassUtil");
    }

    public synchronized final Class<?> forName(String clazz) throws ClassNotFoundException {
        if (destination_instance.get() != null) {
            Class[] classes;
            classes = destinationClasses.get(destination_instance.get());
            if (classes != null) {
                for (Class cls : classes) {
                    if (cls.getName().equals(clazz))
                        return cls;
                }
            }
        }
        throw new ClassNotFoundException(clazz);
    }

    public synchronized final void registerClasses(String destination, Class[] classes) {
        log.debug("Register classes to \"" + destination + "\": " + Arrays.toString(classes));
        destinationClasses.put(destination, classes);
    }

    public synchronized final void unregisterClasses(String destination) {
        log.debug("Unregister classes to \"" + destination + "\"");
        destinationClasses.remove(destination);
    }
}
