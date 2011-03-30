package org.granite.osgi.config;

import org.apache.felix.ipojo.annotations.*;

import org.granite.logging.Logger;
import org.granite.osgi.util.Converter;
import org.granite.util.XMap;

import java.util.Collection;
import java.util.Dictionary;
import java.util.LinkedList;

@Component(immediate = true, architecture = true)
public class Destination extends org.granite.config.flex.Destination {

    private static final Logger LOG = Logger.getLogger(Destination.class);

    @Property(mandatory = true)
    public String SERVICE;

    @Property(mandatory = true)
    public Collection<String> CHANNELS;

    @Property
    public String ADAPTER;

    //
    private boolean state = false;

    private Service service;

    protected Destination() {
        super(null,
              new LinkedList<String>(),
              new XMap(),
              new LinkedList<String>(),
              null,
              null);
    }

    @Property(name = "ID", mandatory = true)
    private void setId(String id) {
        this.id = id;
    }

    @Property(name = "PROPERTIES", mandatory = false)
    private void setProperties(Dictionary<String, String> properties) {
        this.properties = Converter.getXMap(properties);
    }


    @Bind(aggregate = true, optional = true)
    private void bindService(Service service) {
        if (service.getId() == this.SERVICE) {
            this.service = service;
            checkState();
        }
    }

    @Unbind
    private void unbindService(Service service) {
        if (service.getId() == this.SERVICE) {
            this.service = null;
            checkState();
        }
    }

    @Bind(aggregate = true, optional = true)
    private void bindAdapter(Adapter adapter) {
        if (adapter.getId() == this.ADAPTER) {
            this.adapter = adapter;
            checkState();
        }
    }

    @Unbind
    private void unbindAdapter(Adapter adapter) {
        if (adapter.getId() == this.ADAPTER) {
            this.adapter = null;
            checkState();
        }
    }

    @Bind(aggregate = true, optional = true)
    private void bindChannel(Channel channel) {
        if (this.CHANNELS.contains(channel.getId())) {
            this.channelRefs.add(channel.getId());
            checkState();
        }
    }

    @Unbind
    private void unbindChannel(Channel channel) {
        if (this.CHANNELS.contains(channel.getId())) {
            this.channelRefs.remove(channel.getId());
            checkState();
        }
    }

    private void checkState() {
        boolean new_state;
        if (service != null && this.channelRefs.size() > 0) {
            new_state = true;
        } else {
            new_state = false;
        }
        if (new_state != this.state) {
            if (this.state)
                starting();
            else
                stopping();

            this.state = new_state;
        }
    }

    public void starting() {
        LOG.debug("Starting Service:" + toString());
    }

    public void stopping() {
        LOG.debug("Stopping Service:" + toString());
    }
}
