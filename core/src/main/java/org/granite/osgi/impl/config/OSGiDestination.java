package org.granite.osgi.impl.config;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.flex.Destination;
import org.granite.logging.Logger;
import org.granite.util.XMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Map;

@Component(name = "org.granite.config.flex.Destination")
@Provides
public class OSGiDestination extends Destination implements IDestination{

    private static final Logger log = Logger.getLogger(OSGiDestination.class);

    @Property(mandatory = true)
    public String SERVICE;

    @Property(mandatory = true)
    public Collection<String> CHANNEL_LIST;

    @Property(mandatory = false)
    public String ADAPTER;

    //
    private boolean state = false;

    private boolean started = false;

    private IService service;

    protected OSGiDestination() {
        super(null, new ArrayList<String>(), XMap.EMPTY_XMAP,
              new ArrayList<String>(), null, null);
    }

    @Validate
    public void starting() {
        started = true;
        checkState();
    }

    @Invalidate
    public void stopping() {
        if (this.state) {
            stop();
            this.state = false;
        }
        started = false;
    }

    @Property(name = "ID", mandatory = true)
    private void setId(String id) {
        this.id = id;
    }

    @Property(name = "PROPERTIES", mandatory = false)
    private void setProperties(Map<String, String> properties) {
        this.properties = new XMap(properties);
    }


    @Bind(aggregate = true, optional = true)
    private void bindService(IService service) {
        if (service.getService().getId() == this.SERVICE) {
            this.service = service;
            checkState();
        }
    }

    @Unbind
    private void unbindService(IService service) {
        if (service.getService().getId() == this.SERVICE) {
            this.service = null;
            checkState();
        }
    }

    @Bind(aggregate = true, optional = true)
    private void bindAdapter(IAdapter adapter) {
        if (adapter.getAdapter().getId() == this.ADAPTER) {
            this.adapter = adapter.getAdapter();
            checkState();
        }
    }

    @Unbind
    private void unbindAdapter(IAdapter adapter) {
        if (adapter.getAdapter().getId() == this.ADAPTER) {
            this.adapter = null;
            checkState();
        }
    }

    @Bind(aggregate = true, optional = true)
    private void bindChannel(IChannel channel) {
        if (this.CHANNEL_LIST.contains(channel.getChannel().getId())) {
            this.channelRefs.add(channel.getChannel().getId());
            checkState();
        }
    }

    @Unbind
    private void unbindChannel(IChannel channel) {
        if (this.CHANNEL_LIST.contains(channel.getChannel().getId())) {
            this.channelRefs.remove(channel.getChannel().getId());
            checkState();
        }
    }

    private void checkState() {
        boolean new_state;
        if (started && service != null && this.channelRefs.size() > 0) {
            new_state = true;
        } else {
            new_state = false;
        }
        if (new_state != this.state) {
            if (new_state)
                start();
            else
                stop();

            this.state = new_state;
        }
    }

    public void start() {
        log.debug("Start Destination:" + this.id);
        service.getService().addDestination(this);
    }

    public void stop() {
        log.debug("Stop Destination:" + this.id);
        if (service != null) {
            service.getService().removeDestination(this.id);
        }
    }

    public Destination getDestination() {
        return this;
    }
}
