package org.granite.gravity.osgi.impl.config;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.gravity.GravityConfig;
import org.granite.gravity.generic.GenericChannelFactory;
import org.granite.logging.Logger;

import org.xml.sax.SAXException;

import java.io.IOException;


@Component
@Instantiate
@Provides
public class OSGiGravityConfig extends GravityConfig implements IGravityConfig {

    private static final Logger log = Logger.getLogger(OSGiGravityConfig.class);

    OSGiGravityConfig() throws IOException, SAXException {
        super(null, new GenericChannelFactory());
    }

    public GravityConfig getGravityConfig() {
        return this;
    }

    @Validate
    public void starting() {
        log.debug("Start GravityConfig");
    }

    @Invalidate
    public void stopping() {
        log.debug("Stop GravityConfig");
    }
}

