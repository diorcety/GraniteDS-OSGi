package org.granite.osgi.impl;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.GraniteConfig;
import org.granite.config.flex.ServicesConfig;
import org.granite.config.flex.ServicesConfigComponent;
import org.granite.context.GraniteContext;
import org.granite.logging.Logger;

import java.util.HashMap;
import java.util.Map;

@Component
@Provides
@Instantiate
public class OSGiGraniteContext extends GraniteContext {

    private static final Logger LOG = Logger.getLogger(
            OSGiGraniteContext.class);

    protected Map<String, Object> applicationMap = null;

    @Requires
    private ServicesConfigComponent servicesConfig;
    private GraniteConfig graniteConfig;

    public static GraniteContext getThreadIntance(
            GraniteContext graniteContext) {
        setCurrentInstance(graniteContext);
        return graniteContext;

    }

    public OSGiGraniteContext(GraniteConfig graniteConfig,
                              ServicesConfig servicesConfig) {
        super(null, null);
        this.servicesConfig = servicesConfig;
        this.graniteConfig = graniteConfig;
    }

    // OSGi Contructor
    private OSGiGraniteContext() {
        super(null, null);
        try {
            graniteConfig = new GraniteConfig(null, null, null, null);
        } catch (Exception ex) {

        }
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

    @Validate
    public void starting() {
        LOG.debug("Start GraniteContext");
    }

    @Invalidate
    public void stopping() {
        LOG.debug("Stop GraniteContext");
    }
}
