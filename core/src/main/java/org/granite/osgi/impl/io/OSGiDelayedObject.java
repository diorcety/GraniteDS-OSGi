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
