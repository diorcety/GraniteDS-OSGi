package org.granite.context;

import java.util.Map;

public interface GraniteContextComponent {
    public org.granite.config.flex.ServicesConfig getServicesConfig();

    public org.granite.config.GraniteConfig getGraniteConfig();

    public org.granite.context.AMFContext getAMFContext();

    public Object getSessionLock();

    public Map<String, String> getInitialisationMap();

    public Map<String, Object> getApplicationMap();

    public Map<String, Object> getSessionMap();

    public Map<String, Object> getSessionMap(boolean b);

    public Map<String, Object> getRequestMap();
    public GraniteContext getGraniteContext();
}
