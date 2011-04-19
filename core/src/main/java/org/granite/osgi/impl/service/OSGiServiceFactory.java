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
import org.apache.felix.ipojo.annotations.*;
import org.granite.config.flex.Destination;
import org.granite.context.GraniteContext;
import org.granite.context.GraniteManager;
import org.granite.logging.Logger;
import org.granite.messaging.service.DefaultServiceExceptionHandler;
import org.granite.messaging.service.ServiceException;
import org.granite.messaging.service.ServiceExceptionHandler;
import org.granite.messaging.service.ServiceFactory;
import org.granite.osgi.service.GraniteDestination;
import org.granite.util.XMap;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

@Component
@Provides
@Instantiate
public class OSGiServiceFactory implements ServiceFactory {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(OSGiServiceFactory.class);

    private Map<String, GraniteDestination> osgiServices = new Hashtable<String, GraniteDestination>();

    private ServiceExceptionHandler serviceExceptionHandler;

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
    }

    @Bind(aggregate = true, optional = true)
    public final void bindDestination(final GraniteDestination destination) {
        synchronized (osgiServices) {
            osgiServices.put(destination.getId(), destination);
        }
    }

    @Unbind
    public final void unbindDestination(final GraniteDestination destination) {
        synchronized (osgiServices) {
            osgiServices.remove(destination.getId());
        }
    }

    @Override
    public void configure(XMap properties) throws ServiceException {
    }

    @Override
    public ObjectServiceInvoker getServiceInstance(RemotingMessage request) throws ServiceException {
        String messageType = request.getClass().getName();
        String destinationId = request.getDestination();

        GraniteContext context = GraniteManager.getCurrentInstance();
        Destination destination = context.getServicesConfig().findDestinationById(messageType, destinationId);
        if (destination == null)
            throw new ServiceException("No matching destination: " + destinationId);

        Map<String, Object> cache = Collections.synchronizedMap(context.getApplicationMap());

        String key = OSGiServiceFactory.class.getName() + '.' + destination.getId();

        ObjectServiceInvoker service = (ObjectServiceInvoker) cache.get(key);
        // Check update in configuration
        if (service != null && service.getDestination() != destination) {
            service = null;
            log.info("Flush \"" + key + "\" from cache");
        }

        if (service == null) {
            GraniteDestination gd;
            synchronized (osgiServices) {
                gd = osgiServices.get(destination.getId());
            }
            if (gd == null)
                throw new ServiceException("Could not get OSGi destination: " + destination.getId());

            service = new ObjectServiceInvoker<OSGiServiceFactory>(destination, this, gd);
            cache.put(key, service);
        }
        return service;
    }

    public ServiceExceptionHandler getServiceExceptionHandler() {
        return serviceExceptionHandler;
    }
}
