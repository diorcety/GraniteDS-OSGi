package org.granite.osgi.impl.service;

import flex.messaging.messages.RemotingMessage;
import org.granite.config.flex.Destination;
import org.granite.config.flex.Factory;
import org.granite.context.GraniteContext;
import org.granite.logging.Logger;
import org.granite.messaging.service.*;
import org.granite.osgi.service.GraniteFactory;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

public class OSGiFactoryAbstraction extends ServiceFactory {

    private static final Logger log = Logger.getLogger(OSGiFactoryAbstraction.class);

    private final ServiceExceptionHandler serviceExceptionHandler;
    private final GraniteFactory graniteFactory;
    private final Factory factory;

    OSGiFactoryAbstraction(GraniteFactory graniteFactory, Factory factory) {
        this.graniteFactory = graniteFactory;
        this.factory = factory;
        this.serviceExceptionHandler = new DefaultServiceExceptionHandler();
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

        ObjectServiceInvoker service = (ObjectServiceInvoker) cache.get(key);

        // Check update in configuration
        if (service != null && service.getDestination() != destination) {
            service = null;
            log.info("Flush \"" + key + "\" from cache");
        }

        if (service == null) {
            service = new ObjectServiceInvoker<OSGiFactoryAbstraction>(destination, this, graniteFactory.newInstance());
            cache.put(key, service);
        }
        return service;
    }

    public Factory getFactory() {
        return factory;
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
