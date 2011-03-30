package org.granite.osgi.config;

import org.apache.felix.ipojo.annotations.*;

import org.granite.logging.Logger;

import java.util.Collection;
import java.util.HashMap;

@Component(immediate = true, architecture = true)
@Provides
public class Service extends org.granite.config.flex.Service {

    private static final Logger LOG = Logger.getLogger(Service.class);

    @Property
    public Collection<String> ADAPTERS;

    @Property
    public String DEFAULT_ADAPTER;

    //
    @ServiceController
    private boolean state = false;

    public Service() {
        super(null,
              null,
              null,
              null,
              new HashMap<String, org.granite.config.flex.Adapter>(),
              new HashMap<String, org.granite.config.flex.Destination>());
    }

    @Property(name = "ID", mandatory = true)
    private void setId(String id) {
        this.id = id;
    }

    @Property(name = "MESSAGETYPES", mandatory = false,
              value = "flex.messaging.messages.RemotingMessage")
    private void setMessageTypes(String messageTypes) {
        this.messageTypes = messageTypes;
    }

    @Property(name = "CLASS", mandatory = false,
              value = "flex.messaging.services.RemotingService")
    private void setClass(String className) {
        this.className = className;
    }

    @Bind(aggregate = true, optional = true)
    private void bindAdapter(Adapter adapter) {
        if (this.ADAPTERS != null && this.ADAPTERS.contains(adapter.getId())) {
            this.adapters.put(adapter.getId(), adapter);

            if (this.DEFAULT_ADAPTER != null &&
                    this.DEFAULT_ADAPTER.equals(adapter.getId())) {
                this.defaultAdapter = adapter;
            }
            checkState();
        }
    }

    @Unbind
    private void unbindAdapter(Adapter adapter) {
        if (this.ADAPTERS != null && this.ADAPTERS.contains(adapter.getId())) {
            this.adapters.remove(adapter.getId());

            if (this.DEFAULT_ADAPTER != null &&
                    this.DEFAULT_ADAPTER.equals(adapter.getId())) {
                this.defaultAdapter = null;
            }
            checkState();
        }
    }

    private void checkState() {
        boolean new_state;
        if (adapters == null || adapters.size() == ADAPTERS.size()) {
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
