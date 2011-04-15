package org.granite.osgi.impl;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.GraniteConfig;
import org.granite.config.flex.IServicesConfig;
import org.granite.context.AbstractGraniteContext;
import org.granite.logging.Logger;
import org.granite.messaging.service.IMainFactory;

import java.util.HashMap;
import java.util.Map;

@Component
@Provides
@Instantiate
public class OSGiGraniteContext extends AbstractGraniteContext {

    private static final Logger log = Logger.getLogger(OSGiGraniteContext.class);

    protected Map<String, Object> applicationMap = null;

    @Requires
    private IMainFactory mainFactory;

    @Requires
    private IServicesConfig servicesConfig;

    private GraniteConfig graniteConfig;

    private OSGiGraniteContext() {
        super(null, null);
        try {
            graniteConfig = new OSGiGraniteConfig();
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
    public IServicesConfig getServicesConfig() {
        return servicesConfig;
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
    public IMainFactory getMainFactory() {
        return mainFactory;
    }
}
