package org.granite.osgi.impl;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.context.IGraniteClassLoader;
import org.granite.logging.Logger;
import org.granite.osgi.GraniteClassRegistry;

import java.util.Hashtable;
import java.util.Map;

@Component
@Provides
@Instantiate
public class OSGiClassUtil implements GraniteClassRegistry, IGraniteClassLoader {

    private static final Logger log = Logger.getLogger(OSGiClassUtil.class);

    private Map<String, Class<?>> classMap = new Hashtable<String, Class<?>>();

    @Validate
    private void starting() {
        log.debug("Start OSGiClassUtil");
    }

    @Invalidate
    private void stopping() {
        log.debug("Stop OSGiClassUtil");
    }

    public Class<?> forName(String type) throws ClassNotFoundException {
        if (!classMap.containsKey(type))
            throw new ClassNotFoundException(type);
        return classMap.get(type);
    }

    public final synchronized void registerClass(Class<?> clazz) {
        log.info("Register class \"" + clazz.getName() + "\"");
        classMap.put(clazz.getName(), clazz);
    }

    public final synchronized void unregisterClass(Class<?> clazz) {
        log.info("Unregister class \"" + clazz.getName() + "\"");
        classMap.remove(clazz.getName());
    }
}
