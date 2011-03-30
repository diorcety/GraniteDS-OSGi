package org.granite.osgi.config;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;

import org.apache.felix.ipojo.annotations.Validate;
import org.granite.logging.Logger;
import org.granite.osgi.util.Converter;
import org.granite.util.XMap;

import java.util.Dictionary;

@Component(immediate = true, architecture = true)
public class Factory extends org.granite.config.flex.Factory {

    private static final Logger LOG = Logger.getLogger(Factory.class);

    public Factory() {
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
         LOG.debug("Starting Factory:" + toString());
    }

    @Invalidate
    public void stopping() {
         LOG.debug("Stopping Factory:" + toString());
    }
}
