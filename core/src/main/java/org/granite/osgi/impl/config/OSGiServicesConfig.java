package org.granite.osgi.impl.config;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.flex.ServicesConfig;
import org.granite.logging.Logger;
import org.xml.sax.SAXException;

import java.io.IOException;

@Component(publicFactory = false)
@Instantiate
@Provides
public class OSGiServicesConfig extends ServicesConfig implements IServicesConfig{

    private static final Logger log = Logger.getLogger(
            OSGiServicesConfig.class);

    public OSGiServicesConfig() throws IOException, SAXException {
        super(null,null, false);
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
