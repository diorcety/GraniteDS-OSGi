package org.granite.osgi.impl;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.flex.Factory;
import org.granite.config.flex.IServicesConfig;
import org.granite.logging.Logger;
import org.granite.util.XMap;

import java.util.Dictionary;

@Component(name = "org.granite.config.flex.Factory")
public class OSGiFactory extends Factory {

    private static final Logger LOG = Logger.getLogger(OSGiFactory.class);

    @Requires
    private IServicesConfig servicesConfig;


    protected OSGiFactory() {
        super(null,null, XMap.EMPTY_XMAP);
    }

    @Property(name = "ID", mandatory = true)
    private void setId(String id) {
        this.id = id;
    }

    @Property(name = "CLASS", mandatory = true)
    private void setClass(String className) {
        this.className = className;
    }

    @Property(name = "PROPERTIES", mandatory = false)
    private void setProperties(Dictionary<String, String> properties) {
        this.properties = new XMap(properties);
    }

    @Validate
    public void starting() {
        start();
    }

    public void start() {
        LOG.debug("Start Factory:" + this.id);
        servicesConfig.addFactory(this);
    }

    @Invalidate
    public void stopping() {
        stop();
    }

    public void stop() {
        LOG.debug("Stop Factory:" + this.id);
        if (servicesConfig != null) {
            servicesConfig.removeFactory(this.id);
        }
    }
}
