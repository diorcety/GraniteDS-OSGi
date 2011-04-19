package org.granite.gravity.osgi.impl.config;

import org.apache.felix.ipojo.annotations.*;

import org.granite.gravity.ChannelFactory;
import org.granite.gravity.GravityConfig;
import org.granite.gravity.SimpleGravityConfig;
import org.granite.logging.Logger;

import org.xml.sax.SAXException;

import java.io.IOException;


@Component
@Instantiate
@Provides
public class OSGiGravityConfig extends SimpleGravityConfig {

    @Requires
    ChannelFactory channelFactory;

    private static final Logger log = Logger.getLogger(OSGiGravityConfig.class);

    OSGiGravityConfig() throws IOException, SAXException {
        super(null, null);
    }

    public GravityConfig getGravityConfig() {
        return this;
    }

    @Override
    public ChannelFactory getChannelFactory() {
        return channelFactory;
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

