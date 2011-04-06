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
import org.apache.felix.ipojo.annotations.*;
import org.granite.config.flex.Destination;
import org.granite.context.GraniteContext;
import org.granite.logging.Logger;
import org.granite.osgi.service.GraniteDestination;

import java.util.*;

/**
 * @author Franck WOLFF
 */
@Component
@Instantiate
public class SimpleServiceFactory extends ServiceFactory {

    private static final long serialVersionUID = 1L;


    private static final Logger LOG = Logger.getLogger(
            SimpleServiceFactory.class);

    private Map<String, GraniteDestination> osgiServices =
            new Hashtable<String, GraniteDestination>();


    @Bind(aggregate = true, optional = true)
    public final synchronized void bindDestination(
            final GraniteDestination destination) {
        osgiServices.put(destination.getId(), destination);
    }

    @Unbind
    public final synchronized void unbindDestination(
            final GraniteDestination destination) {
        osgiServices.remove(destination.getId());
    }

    @Validate
    private void starting() {
        ServiceFactory.DEFAULT = this;
        LOG.debug("Start OSGiServiceFactory");
    }

    @Invalidate
    private void stopping() {
        LOG.debug("Stop OSGiServiceFactory");
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

        String key = SimpleServiceInvoker.class.getName() + '.' + destination.getId();

        ServiceInvoker service = (SimpleServiceInvoker) cache.get(key);
        if (service == null) {
            if (destination.getProperties().get("OSGi") == null) {
                service = new SimpleServiceInvoker(destination, this);
            } else {
                GraniteDestination gd = osgiServices.get(destination.getId());
                service = new OSGiServiceInvoker(destination, this, gd);
            }
            cache.put(key, service);
        }
        return service;
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
