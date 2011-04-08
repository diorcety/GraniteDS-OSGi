package org.granite.osgi.impl;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.flex.IServicesConfig;
import org.granite.config.flex.SimpleChannel;
import org.granite.config.flex.SimpleEndPoint;
import org.granite.logging.Logger;
import org.granite.util.XMap;

import java.util.Dictionary;

@Component(name = "org.granite.config.flex.Channel")
@Provides
public class OSGiChannel extends SimpleChannel {

    private static final Logger LOG = Logger.getLogger(OSGiChannel.class);

    @Requires
    private IServicesConfig servicesConfig;

    public String ENDPOINT_URI;

    public String ENDPOINT_CLASS;

    protected OSGiChannel() {
        super(null, null, null, XMap.EMPTY_XMAP);
    }

    @Property(name = "ID", mandatory = true)
    private void setId(String id) {
        this.id = id;
    }

    @Property(name = "CLASS", mandatory = false,
              value = "mx.messaging.channels.AMFChannel")
    private void setClass(String className) {
        this.className = className;
    }

    @Property(name = "ENDPOINT_URI", mandatory = true)
    private void setEndPointURI(String epURI) {
        this.ENDPOINT_URI = epURI;
        this.endPoint = new SimpleEndPoint(ENDPOINT_URI, ENDPOINT_CLASS);
    }

    @Property(name = "ENDPOINT_CLASS", mandatory = false,
              value = "flex.messaging.endpoints.AMFEndpoint")
    private void setEndPointClass(String epClass) {
        this.ENDPOINT_CLASS = epClass;
        this.endPoint = new SimpleEndPoint(ENDPOINT_URI, ENDPOINT_CLASS);
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
        LOG.debug("Start Channel:" + this.id);
        servicesConfig.addChannel(this);
    }

    @Invalidate
    public void stopping() {
        stop();
    }

    public void stop() {
        LOG.debug("Stop Channel:" + this.id);
        if (servicesConfig != null) {
            servicesConfig.removeChannel(this.id);
        }
    }
}
