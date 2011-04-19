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
import org.granite.messaging.service.MainFactory;
import org.granite.osgi.GraniteClassRegistry;
import org.granite.osgi.impl.config.IGraniteConfig;
import org.granite.osgi.impl.config.IServicesConfig;
import org.granite.osgi.impl.service.IMainFactory;

import java.util.HashMap;
import java.util.Map;

@Component
@Provides
@Instantiate
public class OSGiGraniteContext extends GraniteContext implements IGraniteContext {

    private static final Logger log = Logger.getLogger(OSGiGraniteContext.class);

    protected Map<String, Object> applicationMap = null;

    @Requires
    private IServicesConfig servicesConfig;

    @Requires
    private IGraniteConfig graniteConfig;

    @Requires
    private IMainFactory mainFactory;

    @Requires
    private GraniteClassRegistry classRegistry;

    private OSGiGraniteContext() {
        super(null, null);
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
        return graniteConfig.getGraniteConfig();
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

    @Override
    public Class<?> forName(String type) throws ClassNotFoundException {
        return classRegistry.forName(type);
    }

    @Override
    public MainFactory getMainFactory() {
        return mainFactory.getMainFactory();
    }
}
