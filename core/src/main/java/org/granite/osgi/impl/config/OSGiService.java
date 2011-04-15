package org.granite.osgi.impl.config;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceController;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.flex.IAdapter;
import org.granite.config.flex.IDestination;
import org.granite.config.flex.IServicesConfig;
import org.granite.config.flex.SimpleService;
import org.granite.logging.Logger;

import java.util.Collection;
import java.util.HashMap;

@Component(name = "org.granite.config.flex.Service")
@Provides
public class OSGiService extends SimpleService {

    private static final Logger LOG = Logger.getLogger(OSGiService.class);

    @Requires
    private IServicesConfig servicesConfig;

    @Property
    public Collection<String> ADAPTER_LIST;

    @Property
    public String DEFAULT_ADAPTER;

    //
    @ServiceController
    private boolean state = false;

    private boolean started = false;

    public OSGiService() {
        super(null, null, null, null, new HashMap<String, IAdapter>(),
              new HashMap<String, IDestination>());
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
    private void bindAdapter(IAdapter adapter) {
        if (this.ADAPTER_LIST != null && this.ADAPTER_LIST.contains(adapter.getId())) {
            this.adapters.put(adapter.getId(), adapter);

            if (this.DEFAULT_ADAPTER != null &&
                    this.DEFAULT_ADAPTER.equals(adapter.getId())) {
                this.defaultAdapter = adapter;
            }
            checkState();
        }
    }

    @Unbind
    private void unbindAdapter(IAdapter adapter) {
        if (this.ADAPTER_LIST != null && this.ADAPTER_LIST.contains(
                adapter.getId())) {
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
        if (started && (adapters == null || ADAPTER_LIST == null
                || adapters.size() == ADAPTER_LIST.size())) {
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
        LOG.debug("Start Service:" + this.id);
        destinations.clear();
        servicesConfig.addService(this);
    }

    public void stop() {
        LOG.debug("Stop Service:" + this.id);
        if (servicesConfig != null) {
            servicesConfig.removeService(this.id);
        }
    }
}
