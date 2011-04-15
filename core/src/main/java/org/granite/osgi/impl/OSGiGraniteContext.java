package org.granite.osgi.impl;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.GraniteConfig;
import org.granite.config.flex.ServicesConfig;
import org.granite.context.GraniteContext;
import org.granite.logging.Logger;
import org.granite.osgi.impl.config.IServicesConfig;

import java.util.HashMap;
import java.util.Map;

@Component
@Provides
@Instantiate
public class OSGiGraniteContext extends GraniteContext implements IGraniteContext{

    private static final Logger log = Logger.getLogger(OSGiGraniteContext.class);

    protected Map<String, Object> applicationMap = null;

    @Requires
    private IServicesConfig servicesConfig;

    private GraniteConfig graniteConfig;

    private OSGiGraniteContext() {
        super(null, null);
        try {
            graniteConfig = new GraniteConfig(null, null, null, null);
        } catch (Exception e) {
            log.error(e, "Can't create GraniteContext");
        }
    }

    @Validate
    public void starting() {
        log.debug("Start GraniteContext");
    }

    @Invalidate
    public void stopping() {
        log.debug("Stop GraniteContext");
    }

    @Override
    public ServicesConfig getServicesConfig() {
        return servicesConfig.getServicesConfig();
    }

    @Override
    public GraniteConfig getGraniteConfig() {
        return graniteConfig;
    }

    @Override
    public Object getSessionLock() {
        return null;
    }

    @Override
    public Map<String, String> getInitialisationMap() {
        return null;
    }

    @Override
    public Map<String, Object> getApplicationMap() {
        if (applicationMap == null)
            applicationMap = new HashMap<String, Object>();
        return applicationMap;
    }

    @Override
    public Map<String, Object> getSessionMap(boolean create) {
        return null;
    }

    @Override
    public Map<String, Object> getSessionMap() {
        return null;
    }

    @Override
    public Map<String, Object> getRequestMap() {
        return null;
    }

    @Override
    public GraniteContext getGraniteContext() {
        return this;
    }
}
