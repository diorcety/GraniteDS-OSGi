package org.granite.config.flex;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.logging.Logger;

import java.util.Hashtable;

@Component(publicFactory = false)
@Instantiate
@Provides
public class OSGiServicesConfig extends ServicesConfig {

    private static final Logger LOG = Logger.getLogger(
            OSGiServicesConfig.class);

    public OSGiServicesConfig() {
    }

    @Validate
    public void starting() {
        LOG.debug("Start ServicesConfig");
    }

    @Invalidate
    public void stopping() {
        LOG.debug("Stop ServicesConfig");
    }
}
