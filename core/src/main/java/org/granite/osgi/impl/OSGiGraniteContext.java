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
import org.granite.context.IGraniteClassLoader;
import org.granite.logging.Logger;
import org.granite.messaging.service.IMainFactory;

import java.util.HashMap;
import java.util.Map;

@Component
@Provides
@Instantiate
public class OSGiGraniteContext extends AbstractGraniteContext {

    private static final Logger LOG = Logger.getLogger(
            OSGiGraniteContext.class);

    protected Map<String, Object> applicationMap = null;

    @Requires
    private IGraniteClassLoader graniteClassLoader;

    @Requires
    private IMainFactory mainFactory;

    @Requires
    private IServicesConfig servicesConfig;

    private GraniteConfig graniteConfig;

    public OSGiGraniteContext(GraniteConfig graniteConfig, IServicesConfig servicesConfig) {
        super(null, null);
        this.servicesConfig = servicesConfig;
        this.graniteConfig = graniteConfig;
    }

    private OSGiGraniteContext() {
        super(null, null);
        try {
            graniteConfig = new GraniteConfig(null, null, null, null);
        } catch (Exception ex) {

        }
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
    public IGraniteClassLoader getClassLoader() {
        return graniteClassLoader;
    }

    @Override
    public IMainFactory getMainFactory()
    {
        return mainFactory;
    }

    @Validate
    public void starting() {
        LOG.debug("Start GraniteContext");
    }

    @Invalidate
    public void stopping() {
        LOG.debug("Stop GraniteContext");
    }
}
