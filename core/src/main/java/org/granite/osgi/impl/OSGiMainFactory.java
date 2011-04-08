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
import org.granite.messaging.service.IMainFactory;
import org.granite.messaging.service.IServiceFactory;
import org.granite.messaging.service.ServiceException;
import org.granite.osgi.service.GraniteFactory;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Provides
@Instantiate
public class OSGiMainFactory implements IMainFactory {

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
    public final synchronized void bindFactory(final GraniteFactory factory) {
        osgiServices.put(factory.getId(), factory);
    }

    @Unbind
    public final synchronized void unbindFactory(final GraniteFactory factory) {
        osgiServices.remove(factory.getId());
    }

    ///////////////////////////////////////////////////////////////////////////
    public IServiceFactory getFactoryInstance(RemotingMessage request) throws ServiceException {

        IGraniteContext context = GraniteContext.getCurrentInstance();

        String messageType = request.getClass().getName();
        String destinationId = request.getDestination();

        log.debug(
                ">> Finding factoryId for messageType: %s and destinationId: %s",
                messageType, destinationId);

        IDestination destination = context.getServicesConfig().findDestinationById(messageType, destinationId);
        if (destination == null)
            throw new ServiceException(
                    "Destination not found: " + destinationId);
        String factoryId = destination.getProperties().get("factory");

        log.debug(">> Found factoryId: %s", factoryId);

        Map<String, Object> cache = context.getApplicationMap();
        String key = OSGiMainFactory.class.getName() + '.' + factoryId;

        return getServiceFactory(cache, context, factoryId, key);
    }

    private IServiceFactory getServiceFactory(Map<String, Object> cache, IGraniteContext context, String factoryId, String key) {
        lock.lock();
        try {

            IServiceFactory factory = (IServiceFactory) cache.get(key);
            if (factory == null) {

                log.debug(">> No cached factory for: %s", factoryId);

                IFactory config = context.getServicesConfig().findFactoryById(factoryId);

                if (config == null) {
                    factory = osgiServiceFactory;
                } else {
                        factory = osgiServices.get(config.getId());
                        if (factory == null)
                            throw new ServiceException("Could not get OSGi factory: " + factoryId);
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
}
