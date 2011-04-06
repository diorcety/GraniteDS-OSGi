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

package org.granite.messaging.service;

import flex.messaging.messages.RemotingMessage;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.annotations.*;

import org.granite.config.flex.Destination;
import org.granite.context.GraniteContext;
import org.granite.logging.Logger;
import org.granite.osgi.service.ServiceDestination;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

@Component
@Provides(specifications = org.granite.osgi.service.ServiceFactory.class)
@Instantiate
public class OSGiServiceFactory extends
        org.granite.osgi.service.ServiceFactory {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(
            OSGiServiceFactory.class);

    private Map<String, ServiceDestination> osgiServices =
            new Hashtable<String, ServiceDestination>();

    private ComponentInstance configuration;

    @Requires(from = "org.granite.config.flex.Factory")
    Factory factoryFactory;

    @Bind(aggregate = true, optional = true)
    public final synchronized void bindDestination(
            final ServiceDestination destination) {
        osgiServices.put(destination.getId(), destination);
    }

    @Unbind
    public final synchronized void unbindDestination(
            final ServiceDestination destination) {
        osgiServices.remove(destination.getId());
    }

    @Override
    public ServiceInvoker<?> getServiceInstance(
            RemotingMessage request) throws ServiceException {
        String messageType = request.getClass().getName();
        String destinationId = request.getDestination();

        GraniteContext context = GraniteContext.getCurrentInstance();
        Destination destination = context.getServicesConfig().findDestinationById(
                messageType, destinationId);
        if (destination == null)
            throw new ServiceException(
                    "No matching destination: " + destinationId);

        Map<String, Object> cache = getCache(destination);

        String key = OSGiServiceFactory.class.getName() + '.' + destination.getId();

        ServiceInvoker service = (ServiceInvoker) cache.get(key);
        if (service == null) {
            ServiceDestination sd = osgiServices.get(destination.getId());
            if (sd != null) {
                service = new OSGiServiceInvoker(destination, this, sd);
                cache.put(key, service);
            }
        }

        return service;
    }

    @Validate
    private void starting() {
        LOG.debug("Start OSGiServiceFactory");
        try {
            Dictionary properties = new Hashtable();
            properties.put("ID", "OSGiFactory");
            properties.put("CLASS", OSGiServiceFactory.class.getName());
            configuration = factoryFactory.createComponentInstance(properties);
        } catch (Exception e) {
            LOG.error(e, "Failed to create OSGiServiceFactory configuration");
        }
    }

    @Invalidate
    private void stopping() {
        LOG.debug("Stop OSGiServiceFactory");
    }

    private Map<String, Object> getCache(
            Destination destination) throws ServiceException {
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
            throw new ServiceException(
                    "Illegal scope in destination: " + destination);

        return cache;
    }
}
