package org.granite.osgi.impl;

import flex.messaging.messages.RemotingMessage;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.flex.IDestination;
import org.granite.config.flex.IFactory;
import org.granite.context.GraniteContext;
import org.granite.context.IGraniteContext;
import org.granite.logging.Logger;
import org.granite.messaging.service.*;
import org.granite.osgi.service.GraniteFactory;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Provides
@Instantiate
public class OSGiMainFactory implements IMainFactory {

    private static final Logger LOG = Logger.getLogger(OSGiMainFactory.class);

    private final ReentrantLock lock = new ReentrantLock();

    private Map<String, GraniteFactory> osgiServices = new Hashtable<String, GraniteFactory>();

    private Map<String, CacheEntry> cacheEntries = new Hashtable<String, CacheEntry>();

    @Requires
    IServiceFactory osgiServiceFactory;

    @Validate
    private void starting() {
        LOG.debug("Start MainFactory");
    }

    @Invalidate
    private void stopping() {
        LOG.debug("Stop MainFactory");

        // Remove cache entries
        for (Iterator<CacheEntry> ice = cacheEntries.values().iterator(); ice.hasNext();) {
            CacheEntry ce = ice.next();
            LOG.info("Remove \"" + ce.entry + "\" from the cache");
            OSGiFactoryAbstraction service = (OSGiFactoryAbstraction) ce.cache.remove(ce.entry);
            service.remove();
        }
    }

    @Bind(aggregate = true, optional = true)
    public final synchronized void bindFactory(final GraniteFactory factory) {
        osgiServices.put(factory.getId(), factory);
    }

    @Unbind
    public final synchronized void unbindFactory(final GraniteFactory factory) {
        osgiServices.remove(factory.getId());
    }

    @Bind(aggregate = true, optional = true)
    public final synchronized void bindFactoryConfiguration(final IFactory factory) {

    }

    @Unbind
    public final synchronized void unbindFactoryConfiguration(final IFactory factory) {
        CacheEntry ce = cacheEntries.remove(factory.getId());
        if (ce != null) {
            LOG.info("Remove \"" + ce.entry + "\" (" + factory.getId() + ") from the cache");
            OSGiFactoryAbstraction service = (OSGiFactoryAbstraction) ce.cache.remove(ce.entry);
            service.remove();
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    public IServiceFactory getFactoryInstance(RemotingMessage request) throws ServiceException {

        IGraniteContext context = GraniteContext.getCurrentInstance();

        String messageType = request.getClass().getName();
        String destinationId = request.getDestination();

        LOG.debug(
                ">> Finding factoryId for messageType: %s and destinationId: %s",
                messageType, destinationId);

        IDestination destination = context.getServicesConfig().findDestinationById(messageType, destinationId);
        if (destination == null)
            throw new ServiceException(
                    "Destination not found: " + destinationId);
        String factoryId = destination.getProperties().get("factory");

        LOG.debug(">> Found factoryId: %s", factoryId);

        String key = OSGiMainFactory.class.getName() + '.' + factoryId;

        return getServiceFactory(context, factoryId, key);
    }

    private IServiceFactory getServiceFactory(IGraniteContext context, String factoryId, String key) {
        lock.lock();
        try {
            Map<String, Object> cache = Collections.synchronizedMap(context.getApplicationMap());
            IServiceFactory factory = (IServiceFactory) cache.get(key);
            if (factory == null) {

                LOG.debug(">> No cached factory for: %s", factoryId);

                IFactory config = context.getServicesConfig().findFactoryById(factoryId);

                if (config == null) {
                    factory = osgiServiceFactory;
                } else {
                    GraniteFactory gf = osgiServices.get(config.getId());
                    if (gf == null)
                        throw new ServiceException("Could not get OSGi factory: " + factoryId);
                    factory = new OSGiFactoryAbstraction(gf);
                    cacheEntries.put(config.getId(), new CacheEntry(cache, key));
                }
                cache.put(key, factory);
            } else
                LOG.debug(">> Found a cached factory for: %s", factoryId);

            LOG.debug("<< Returning factory: %s", factory);

            return factory;
        } finally {
            lock.unlock();
        }
    }

}
