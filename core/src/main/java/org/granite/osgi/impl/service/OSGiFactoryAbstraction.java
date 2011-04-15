package org.granite.osgi.impl.service;

import flex.messaging.messages.RemotingMessage;

import org.granite.config.flex.Destination;
import org.granite.context.GraniteContext;
import org.granite.logging.Logger;
import org.granite.messaging.service.DefaultServiceExceptionHandler;
import org.granite.messaging.service.ServiceException;
import org.granite.messaging.service.ServiceExceptionHandler;
import org.granite.messaging.service.ServiceFactory;
import org.granite.messaging.service.ServiceInvoker;
import org.granite.osgi.service.GraniteFactory;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

public class OSGiFactoryAbstraction extends ServiceFactory {

    private static final Logger log = Logger.getLogger(OSGiFactoryAbstraction.class);

    private ServiceExceptionHandler serviceExceptionHandler;
    private final GraniteFactory graniteFactory;
    private Map<String, CacheEntry> cacheEntries = new Hashtable<String, CacheEntry>();

    OSGiFactoryAbstraction(GraniteFactory gf) {
        this.graniteFactory = gf;
        this.serviceExceptionHandler = new DefaultServiceExceptionHandler();
    }

    public void remove() {
        // Remove cache entries
        for (Iterator<CacheEntry> ice = cacheEntries.values().iterator(); ice.hasNext();) {
            try {
                CacheEntry ce = ice.next();
                log.info("Remove \"" + ce.entry + "\" from the cache");
                ce.cache.remove(ce.entry);
            } catch (IllegalStateException e) {
                log.warn("Cache flush exception: " + e.getMessage());
            }
        }
    }

    @Override
    public ServiceInvoker getServiceInstance(RemotingMessage request) throws ServiceException {
        String messageType = request.getClass().getName();
        String destinationId = request.getDestination();

        GraniteContext context = GraniteContext.getCurrentInstance();
        Destination destination = context.getServicesConfig().findDestinationById(messageType, destinationId);
        if (destination == null)
            throw new ServiceException("No matching destination: " + destinationId);

        Map<String, Object> cache = getCache(destination);

        String key = OSGiFactoryAbstraction.class.getName() + '.' + destination.getId();

        ServiceInvoker service = (ServiceInvoker) cache.get(key);
        if (service == null) {
            service = new ObjectServiceInvoker<OSGiFactoryAbstraction>(destination, this, graniteFactory.newInstance());
            cacheEntries.put(destination.getId(), new CacheEntry(cache, key));
            cache.put(key, service);
        }
        return service;
    }

    @Override
    public ServiceExceptionHandler getServiceExceptionHandler() {
        return this.serviceExceptionHandler;
    }

    private Map<String, Object> getCache(Destination destination) throws ServiceException {
        GraniteContext context = GraniteContext.getCurrentInstance();
        String scope = destination.getProperties().get("scope");

        Map<String, Object> cache = null;
        if (scope == null || "request".equals(scope))
            cache = context.getRequestMap();
        else if ("session".equals(scope))
            cache = context.getSessionMap();
        else if ("application".equals(scope))
            cache = Collections.synchronizedMap(context.getApplicationMap());
        else
            throw new ServiceException("Illegal scope in destination: " + destination);

        return cache;
    }
}
