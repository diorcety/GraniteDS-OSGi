package org.granite.osgi.impl;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.GraniteConfig;
import org.granite.config.flex.ServicesConfig;
import org.granite.config.flex.ServicesConfigInterface;
import org.granite.context.GraniteContext;
import org.granite.logging.Logger;
import org.granite.osgi.OSGiGraniteContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component(immediate = true)
@Instantiate
@Provides
public class OSGiGraniteContextImpl
        extends GraniteContext
        implements OSGiGraniteContext {

    private static final Logger LOG = Logger.getLogger(
            OSGiGraniteContextImpl.class);

    protected Map<String, Object> applicationMap = null;

    @Requires(proxy = false)        //HACK
    private ServicesConfigInterface servicesConfig;
    private GraniteConfig graniteConfig;

    public static GraniteContext getThreadIntance(
            GraniteContext graniteContext) {
        setCurrentInstance(graniteContext);
        return graniteContext;

    }

    public OSGiGraniteContextImpl(GraniteConfig graniteConfig,
                                  ServicesConfig servicesConfig) {
        super(null, null);
        this.servicesConfig = servicesConfig;
        this.graniteConfig = graniteConfig;
    }

    // OSGi Contructor
    private OSGiGraniteContextImpl() {
        super(null, null);
        try {
            graniteConfig = new GraniteConfig(null, null, null, null);
        } catch (Exception ex) {

        }
    }

    @Override
    public ServicesConfig getServicesConfig() {
        return (ServicesConfig) servicesConfig;         //HACK
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
