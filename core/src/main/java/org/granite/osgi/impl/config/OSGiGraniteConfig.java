package org.granite.osgi.impl.config;

import org.apache.felix.ipojo.annotations.*;
import org.granite.config.GraniteConfig;
import org.granite.logging.Logger;
import org.granite.messaging.amf.io.util.ActionScriptClassDescriptor;
import org.granite.osgi.impl.io.OSGiActionScriptClassDescriptor;
import org.xml.sax.SAXException;

import java.io.IOException;

@Component
@Instantiate
@Provides
public class OSGiGraniteConfig extends GraniteConfig implements IGraniteConfig {

    private static final Logger log = Logger.getLogger(OSGiGraniteConfig.class);

    OSGiGraniteConfig() throws IOException, SAXException {
        super(null, null, null, null);
    }

    public Class<? extends ActionScriptClassDescriptor> getActionScriptDescriptor(String type) {
        try {
            return super.getActionScriptDescriptor(type);
        } catch (Exception e) {
            return OSGiActionScriptClassDescriptor.class;
        }
    }

    public GraniteConfig getGraniteConfig() {
        return this;
    }

    @Validate
    public void starting() {
        log.debug("Start GraniteConfig");
    }

    @Invalidate
    public void stopping() {
        log.debug("Stop GraniteConfig");
    }
}

