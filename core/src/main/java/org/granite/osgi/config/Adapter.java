package org.granite.osgi.config;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.util.XMap;
import org.granite.logging.Logger;
import org.granite.osgi.util.Converter;

import java.util.Dictionary;

@Component(immediate = true, architecture = true)
@Provides
public class Adapter extends org.granite.config.flex.Adapter {

    private static final Logger LOG = Logger.getLogger(Adapter.class);

    protected Adapter() {
        super(null, null, new XMap());
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
        this.properties = Converter.getXMap(properties);
    }

    @Validate
    public void starting() {
        LOG.debug("Starting Adapter:" + toString());
    }

    @Invalidate
    public void stopping() {
        LOG.debug("Stopping Adapter:" + toString());
    }
}
