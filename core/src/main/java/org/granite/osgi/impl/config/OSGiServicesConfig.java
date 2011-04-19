package org.granite.osgi.impl.config;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.flex.ServicesConfig;
import org.granite.config.flex.SimpleServicesConfig;
import org.granite.logging.Logger;
import org.xml.sax.SAXException;

import java.io.IOException;

@Component
@Instantiate
@Provides
public class OSGiServicesConfig extends SimpleServicesConfig {

    private static final Logger log = Logger.getLogger(
            OSGiServicesConfig.class);

    public OSGiServicesConfig() throws IOException, SAXException {
        super(null, null, false);
    }

    public ServicesConfig getServicesConfig() {
        return this;
    }

    @Validate
    public void starting() {
        log.debug("Start ServicesConfig");
    }

    @Invalidate
    public void stopping() {
        log.debug("Stop ServicesConfig");
    }
}
