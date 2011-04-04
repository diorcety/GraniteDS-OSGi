package org.granite.osgi;

import org.granite.config.GraniteConfig;
import org.granite.config.flex.ServicesConfig;
import org.granite.context.AMFContext;
import org.granite.context.GraniteContext;

import java.util.Map;

public interface OSGiGraniteContext {

    public AMFContext getAMFContext();

    public GraniteConfig getGraniteConfig();

    public ServicesConfig getServicesConfig();

    public Object getSessionLock();

    public Map<String, String> getInitialisationMap();

    public Map<String, Object> getApplicationMap();

    public Map<String, Object> getSessionMap();

    public Map<String, Object> getSessionMap(boolean create);

    public Map<String, Object> getRequestMap();
}
