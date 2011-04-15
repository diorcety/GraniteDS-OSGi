package org.granite.osgi.impl.config;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.flex.Channel;
import org.granite.config.flex.EndPoint;
import org.granite.logging.Logger;
import org.granite.util.XMap;

import java.util.Dictionary;
import java.util.Map;

@Component(name = "org.granite.config.flex.Channel")
@Provides
public class OSGiChannel extends Channel implements IChannel{

    private static final Logger log = Logger.getLogger(OSGiChannel.class);

    @Requires
    private IServicesConfig servicesConfig;

    public String ENDPOINT_URI;

    public String ENDPOINT_CLASS;

    protected OSGiChannel() {
        super(null, null, null, XMap.EMPTY_XMAP);
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

    @Property(name = "CLASS", mandatory = false,
              value = "mx.messaging.channels.AMFChannel")
    private void setClass(String className) {
        this.className = className;
    }

    @Property(name = "ENDPOINT_URI", mandatory = true)
    private void setEndPointURI(String epURI) {
        this.ENDPOINT_URI = epURI;
        this.endPoint = new EndPoint(ENDPOINT_URI, ENDPOINT_CLASS);
    }

    @Property(name = "ENDPOINT_CLASS", mandatory = false,
              value = "flex.messaging.endpoints.AMFEndpoint")
    private void setEndPointClass(String epClass) {
        this.ENDPOINT_CLASS = epClass;
        this.endPoint = new EndPoint(ENDPOINT_URI, ENDPOINT_CLASS);
    }


    @Property(name = "PROPERTIES", mandatory = false)
    private void setProperties(Map<String, String> properties) {
        this.properties = new XMap(properties);
    }

    public void start() {
        log.debug("Start Channel:" + this.id);
        servicesConfig.getServicesConfig().addChannel(this);
    }

    public void stop() {
        log.debug("Stop Channel:" + this.id);
        if (servicesConfig != null) {
            servicesConfig.getServicesConfig().removeChannel(this.id);
        }
    }

    public Channel getChannel() {
        return this;
    }
}
