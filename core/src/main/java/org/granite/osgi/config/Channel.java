package org.granite.osgi.config;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.flex.EndPoint;
import org.granite.logging.Logger;
import org.granite.osgi.util.Converter;
import org.granite.util.XMap;

import java.util.Dictionary;

@Component(immediate = true, architecture = true)
@Provides
public class Channel extends org.granite.config.flex.Channel {

    private static final Logger LOG = Logger.getLogger(Channel.class);

    public String ENDPOINT_URI;

    public String ENDPOINT_CLASS;

    public Channel() {
        super(null, null, null, new XMap());
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
    private void setProperties(Dictionary<String, String> properties) {
        this.properties = Converter.getXMap(properties);
    }

    @Validate
    public void starting() {
        LOG.debug("Starting Channel:" + toString());
    }

    @Invalidate
    public void stopping() {
        LOG.debug("Stopping Channel:" + toString());
    }
}
