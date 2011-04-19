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
import org.granite.config.flex.Destination;
import org.granite.config.flex.Factory;
import org.granite.context.GraniteContext;
import org.granite.context.GraniteManager;
import org.granite.logging.Logger;
import org.granite.messaging.service.*;
import org.granite.osgi.service.GraniteFactory;
import org.granite.util.XMap;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

public class OSGiFactoryAbstraction implements ServiceFactory {

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
    public void configure(XMap properties) throws ServiceException {
    }

    @Override
    public ServiceInvoker getServiceInstance(RemotingMessage request) throws ServiceException {
        String messageType = request.getClass().getName();
        String destinationId = request.getDestination();

        GraniteContext context = GraniteManager.getCurrentInstance();
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
        GraniteContext context = GraniteManager.getCurrentInstance();
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
