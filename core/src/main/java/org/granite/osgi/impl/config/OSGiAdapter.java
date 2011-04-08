package org.granite.osgi.impl.config;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.flex.SimpleAdapter;
import org.granite.logging.Logger;
import org.granite.util.XMap;

import java.util.Dictionary;

@Component(name = "org.granite.config.flex.Adapter")
@Provides
public class OSGiAdapter extends SimpleAdapter {

    private static final Logger LOG = Logger.getLogger(OSGiAdapter.class);

    ///////////////////////////////////////////////////////////////////////////
    // OSGi

    protected OSGiAdapter() {
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

    @Property(name = "CLASS", mandatory = true)
    private void setClass(String className) {
        this.className = className;
    }

    @Property(name = "PROPERTIES", mandatory = false)
    private void setProperties(Dictionary<String, String> properties) {
        this.properties = new XMap(properties);
    }

    public void start() {
        LOG.debug("Start Adapter:" + this.id);
    }


    public void stop() {
        LOG.debug("Stop Adapter:" + this.id);
    }
}
