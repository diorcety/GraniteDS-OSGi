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

package org.granite.osgi.impl.io;

import org.granite.util.ClassUtil;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public class OSGiDelayedObject extends HashMap<String, Object> {
    String type;

    OSGiDelayedObject(String type) {
        this.type = type;
    }

    public String getType()
    {
         return type;
    }

    public Object newInstance() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        Object obj = ClassUtil.newInstance(type);
        Class clazz = obj.getClass();
        try {
            for (String name : this.keySet()) {
                boolean find = false;

                // Try to find public getter/setter.
                BeanInfo info = Introspector.getBeanInfo(clazz);
                PropertyDescriptor[] props = info.getPropertyDescriptors();
                for (PropertyDescriptor prop : props) {
                    if (name.equals(prop.getName()) && prop.getWriteMethod() != null && prop.getReadMethod() != null) {
                        prop.getWriteMethod().invoke(obj, this.get(name));
                        find = true;
                    }
                }
                if (!find) {
                    // Try to find public field.
                    Field field = clazz.getField(name);
                    if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers())) {
                        field.setAccessible(true);
                        field.set(obj, this.get(name));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return obj;
    }
}
