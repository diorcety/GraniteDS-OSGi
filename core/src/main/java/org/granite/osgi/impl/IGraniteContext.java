package org.granite.osgi.impl;

import org.granite.config.GraniteConfig;
import org.granite.config.flex.ServicesConfig;
import org.granite.context.AMFContext;
import org.granite.context.GraniteContext;

import java.util.Map;

public interface IGraniteContext {
    public ServicesConfig getServicesConfig();

    public GraniteConfig getGraniteConfig();

    public AMFContext getAMFContext();

    public Object getSessionLock();

    public Map<String, String> getInitialisationMap();

    public Map<String, Object> getApplicationMap();

    public Map<String, Object> getSessionMap();

    public Map<String, Object> getSessionMap(boolean b);

    public Map<String, Object> getRequestMap();

    public GraniteContext getGraniteContext();
}
