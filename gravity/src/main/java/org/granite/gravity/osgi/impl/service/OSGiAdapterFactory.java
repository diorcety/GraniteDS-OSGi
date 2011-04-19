package org.granite.gravity.osgi.impl.service;

import flex.messaging.messages.AsyncMessage;
import flex.messaging.messages.CommandMessage;
import flex.messaging.messages.Message;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.flex.Adapter;
import org.granite.config.flex.Destination;
import org.granite.context.GraniteContext;
import org.granite.gravity.Gravity;
import org.granite.gravity.adapters.AdapterFactory;
import org.granite.gravity.adapters.ServiceAdapter;
import org.granite.logging.Logger;
import org.granite.messaging.service.ServiceException;
import org.granite.osgi.service.GraniteAdapter;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Instantiate
@Provides
public class OSGiAdapterFactory extends AdapterFactory implements IAdapterFactory {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(AdapterFactory.class);

    private static final ReentrantLock lock = new ReentrantLock();

    private Map<String, GraniteAdapter> osgiServices = new Hashtable<String, GraniteAdapter>();

    public OSGiAdapterFactory() {
        super(null);
    }

    @Bind(aggregate = true, optional = true)
    public final void bindAdapter(final GraniteAdapter adapter) {
        synchronized (osgiServices) {
            osgiServices.put(adapter.getId(), adapter);
        }
    }

    @Unbind
    public final void unbindAdapter(final GraniteAdapter adapter) {
        synchronized (osgiServices) {
            osgiServices.remove(adapter.getId());
        }
    }

    public ServiceAdapter getServiceAdapter(Message request) throws ServiceException {

        String messageType = request.getClass().getName();
        if (request instanceof CommandMessage)
            messageType = ((CommandMessage) request).getMessageRefType();
        if (messageType == null)
            messageType = AsyncMessage.class.getName();
        String destinationId = request.getDestination();

        return getServiceAdapter(messageType, destinationId);
    }

    public ServiceAdapter getServiceAdapter(String messageType, String destinationId) throws ServiceException {
        GraniteContext context = GraniteContext.getCurrentInstance();

        log.debug(">> Finding serviceAdapter for messageType: %s and destinationId: %s", messageType, destinationId);

        Destination destination = context.getServicesConfig().findDestinationById(messageType, destinationId);
        if (destination == null) {
            log.debug(">> No destination found: %s", destinationId);
            return null;
        }

        Adapter adapter = destination.getAdapter();
        if (adapter == null)
            throw new ServiceException("No adapter defined: " + destinationId);

        String key = AdapterFactory.class.getName() + '@' + destination.getId() + '.' + adapter.getId();

        return getServiceAdapter(context, destination, key, adapter != null ? adapter.getId() : null);
    }

    private ServiceAdapter getServiceAdapter(GraniteContext context, Destination destination, String key, String adapterId) {
        lock.lock();
        try {
            Map<String, Object> cache = Collections.synchronizedMap(context.getApplicationMap());

            Adapter config = destination.getAdapter();

            ServiceAdapter serviceAdapter = (ServiceAdapter) cache.get(key);

            // Check update in configuration
            if (serviceAdapter != null && serviceAdapter instanceof OSGiAdapterAbstraction) {
                OSGiAdapterAbstraction adapterAbstraction = (OSGiAdapterAbstraction) serviceAdapter;
                if (adapterAbstraction.getAdapter() != config) {
                    serviceAdapter = null;
                    log.info("Flush \"" + key + "\" from cache");
                }
            }

            if (serviceAdapter == null) {
                log.debug(">> No cached factory for: %s", adapterId);

                GraniteAdapter ga;
                synchronized (osgiServices) {
                    ga = osgiServices.get(config.getId());
                }
                if (ga == null)
                    throw new ServiceException("Could not get OSGi adapter: " + destination.getId());
                serviceAdapter = new OSGiAdapterAbstraction(ga, config);

                cache.put(key, serviceAdapter);
            } else
                log.debug(">> Found a cached serviceAdapter for ref: %s", destination.getAdapter());

            log.debug("<< Returning serviceAdapter: %s", serviceAdapter);

            serviceAdapter.setDestination(destination);
            return serviceAdapter;
        } finally {
            lock.unlock();
        }
    }

    @Validate
    public void starting() {
        log.debug("Start OSGiAdapterFactory");
    }

    @Invalidate
    public void stopping() {
        log.debug("Stop OSGiAdapterFactory");
    }

    @Override
    public String toString() {
        return toString(null);
    }

    public String toString(String append) {
        return super.toString() + " {" +
                (append != null ? append : "") +
                "\n}";
    }
}
