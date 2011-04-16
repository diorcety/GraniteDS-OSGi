package org.granite.osgi.impl.service;

import flex.messaging.messages.RemotingMessage;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.flex.Destination;
import org.granite.config.flex.Factory;
import org.granite.context.GraniteContext;
import org.granite.logging.Logger;
import org.granite.messaging.service.*;
import org.granite.osgi.impl.config.IDestination;
import org.granite.osgi.impl.config.IFactory;
import org.granite.osgi.service.GraniteFactory;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Provides
@Instantiate
public class OSGiMainFactory extends MainFactory implements IMainFactory {

    private static final Logger log = Logger.getLogger(OSGiMainFactory.class);

    private final ReentrantLock lock = new ReentrantLock();

    private Map<String, GraniteFactory> osgiServices = new Hashtable<String, GraniteFactory>();

    @Requires
    IServiceFactory osgiServiceFactory;

    @Validate
    private void starting() {
        log.debug("Start MainFactory");
    }

    @Invalidate
    private void stopping() {
        log.debug("Stop MainFactory");
    }

    @Bind(aggregate = true, optional = true)
    public final void bindFactory(final GraniteFactory factory) {
        synchronized (osgiServices) {
            osgiServices.put(factory.getId(), factory);
        }
    }

    @Unbind
    public final void unbindFactory(final GraniteFactory factory) {
        synchronized (osgiServices) {
            osgiServices.remove(factory.getId());
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    public ServiceFactory getFactoryInstance(RemotingMessage request) throws ServiceException {

        GraniteContext context = GraniteContext.getCurrentInstance();

        String messageType = request.getClass().getName();
        String destinationId = request.getDestination();

        log.debug(">> Finding factoryId for messageType: %s and destinationId: %s", messageType, destinationId);

        Destination destination = context.getServicesConfig().findDestinationById(messageType, destinationId);
        if (destination == null)
            throw new ServiceException("Destination not found: " + destinationId);
        String factoryId = destination.getProperties().get("factory");

        log.debug(">> Found factoryId: %s", factoryId);

        String key = OSGiMainFactory.class.getName() + '.' + factoryId;

        return getServiceFactory(context, factoryId, key);
    }

    private ServiceFactory getServiceFactory(GraniteContext context, String factoryId, String key) {
        lock.lock();
        try {
            Map<String, Object> cache = Collections.synchronizedMap(context.getApplicationMap());
            Factory config = context.getServicesConfig().findFactoryById(factoryId);

            ServiceFactory factory = (ServiceFactory) cache.get(key);

            // Check update in configuration
            if (factory != null && factory instanceof OSGiFactoryAbstraction) {
                OSGiFactoryAbstraction factoryAbstraction = (OSGiFactoryAbstraction) factory;
                if (factoryAbstraction.getFactory() != config) {
                    factory = null;
                    log.info("Flush \"" + key + "\" from cache");
                }
            }

            if (factory == null) {

                log.debug(">> No cached factory for: %s", factoryId);


                if (config == null) {
                    factory = osgiServiceFactory.getServiceFactory();
                } else {
                    GraniteFactory graniteFactory;
                    synchronized (osgiServices) {
                        graniteFactory = osgiServices.get(config.getId());
                    }
                    if (graniteFactory == null)
                        throw new ServiceException("Could not get OSGi factory: " + factoryId);
                    factory = new OSGiFactoryAbstraction(graniteFactory, config);
                }
                cache.put(key, factory);
            } else
                log.debug(">> Found a cached factory for: %s", factoryId);

            log.debug("<< Returning factory: %s", factory);

            return factory;
        } finally {
            lock.unlock();
        }
    }

    public MainFactory getMainFactory() {
        return this;
    }
}
