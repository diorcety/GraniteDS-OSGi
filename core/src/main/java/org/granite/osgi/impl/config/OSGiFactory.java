package org.granite.osgi.impl.config;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.flex.Factory;
import org.granite.config.flex.ServicesConfig;
import org.granite.config.flex.SimpleFactory;
import org.granite.logging.Logger;
import org.granite.util.XMap;

import java.util.Map;

@Component(name = "org.granite.config.flex.Factory")
@Provides
public class OSGiFactory extends SimpleFactory {

    private static final Logger log = Logger.getLogger(OSGiFactory.class);

    @Requires
    private ServicesConfig servicesConfig;


    protected OSGiFactory() {
        super(null, null, XMap.EMPTY_XMAP);
    }

    @Validate
    public void starting() {
        start();
    }

    @Invalidate
    public void stopping() {
        stop();
    }

    @Property(name = "ID", mandatory = true)
    private void setId(String id) {
        this.id = id;
    }

    @Property(name = "CLASS", mandatory = false)
    private void setClass(String className) {
        this.className = className;
    }

    @Property(name = "PROPERTIES", mandatory = false)
    private void setProperties(Map<String, String> properties) {
        this.properties = new XMap(properties);
    }

    public void start() {
        log.debug("Start Factory: " + this.id);
        servicesConfig.addFactory(this);
    }

    public void stop() {
        log.debug("Stop Factory: " + this.id);
        if (servicesConfig != null) {
            servicesConfig.removeFactory(this.id);
        }
    }

    public Factory getFactory() {
        return this;
    }
}
