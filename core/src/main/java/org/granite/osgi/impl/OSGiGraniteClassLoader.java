package org.granite.osgi.impl;


public interface OSGiGraniteClassLoader {
        public Class<?> forName(String type) throws ClassNotFoundException;
}
