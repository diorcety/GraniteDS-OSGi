/*
  GRANITE DATA SERVICES
  Copyright (C) 2011 GRANITE DATA SERVICES S.A.S.

  This file is part of Granite Data Services.

  Granite Data Services is free software; you can redistribute it and/or modify
  it under the terms of the GNU Library General Public License as published by
  the Free Software Foundation; either version 2 of the License, or (at your
  option) any later version.

  Granite Data Services is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Library General Public License
  for more details.

  You should have received a copy of the GNU Library General Public License
  along with this library; if not, see <http://www.gnu.org/licenses/>.
*/

package org.granite.osgi.impl.service;

import flex.messaging.messages.RemotingMessage;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.flex.Destination;
import org.granite.context.GraniteContext;
import org.granite.logging.Logger;
import org.granite.messaging.service.DefaultServiceExceptionHandler;
import org.granite.messaging.service.ServiceException;
import org.granite.messaging.service.ServiceExceptionHandler;
import org.granite.messaging.service.ServiceFactory;
import org.granite.osgi.service.GraniteDestination;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Franck WOLFF
 */
@Component
@Provides
@Instantiate
public class OSGiServiceFactory extends ServiceFactory implements IServiceFactory {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(OSGiServiceFactory.class);

    private Map<String, GraniteDestination> osgiServices = new Hashtable<String, GraniteDestination>();

    private ServiceExceptionHandler serviceExceptionHandler;

    private Map<String, CacheEntry> cacheEntries = new Hashtable<String, CacheEntry>();

    OSGiServiceFactory() {
        this.serviceExceptionHandler = new DefaultServiceExceptionHandler();
    }

    @Validate
    private void starting() {
        log.debug("Start OSGiServiceFactory");
    }

    @Invalidate
    private void stopping() {
        log.debug("Stop OSGiServiceFactory");

        // Remove cache entries
        for (Iterator<CacheEntry> ice = cacheEntries.values().iterator(); ice.hasNext();) {
            CacheEntry ce = ice.next();
            log.info("Remove \"" + ce.entry + "\" from the cache");
            ce.cache.remove(ce.entry);
        }
    }

    @Bind(aggregate = true, optional = true)
    public final synchronized void bindDestination(final GraniteDestination destination) {
        osgiServices.put(destination.getId(), destination);
    }

    @Unbind
    public final synchronized void unbindDestination(final GraniteDestination destination) {
        osgiServices.remove(destination.getId());
    }

    @Bind(aggregate = true, optional = true)
    public final synchronized void bindDestinationConfiguration(final Destination destination) {

    }

    @Unbind
    public final synchronized void unbindDestinationConfiguration(final Destination destination) {
        CacheEntry ce = cacheEntries.remove(destination.getId());
        if (ce != null) {
            log.info("Remove \"" + ce.entry + "\" (" + destination.getId() + ") from the cache");
            ce.cache.remove(ce.entry);
        }
    }

    @Override
    public ObjectServiceInvoker getServiceInstance(RemotingMessage request) throws ServiceException {
        String messageType = request.getClass().getName();
        String destinationId = request.getDestination();

        GraniteContext context = GraniteContext.getCurrentInstance();
        Destination destination = context.getServicesConfig().findDestinationById(messageType, destinationId);
        if (destination == null)
            throw new ServiceException("No matching destination: " + destinationId);

        Map<String, Object> cache = Collections.synchronizedMap(context.getApplicationMap());

        String key = OSGiServiceFactory.class.getName() + '.' + destination.getId();

        ObjectServiceInvoker service = (ObjectServiceInvoker) cache.get(key);
        if (service == null) {
            GraniteDestination gd = osgiServices.get(destination.getId());
            if (gd == null)
                throw new ServiceException("Could not get OSGi destination: " + destination.getId());

            service = new ObjectServiceInvoker<OSGiServiceFactory>(destination, this, gd);

            cacheEntries.put(destination.getId(), new CacheEntry(cache, key));
            cache.put(key, service);
        }
        return service;
    }

    public ServiceExceptionHandler getServiceExceptionHandler() {
        return serviceExceptionHandler;
    }

    public ServiceFactory getServiceFactory()
    {
        return this;
    }
}
