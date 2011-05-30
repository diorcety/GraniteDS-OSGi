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

import flex.messaging.messages.Message;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Unbind;

import org.granite.config.flex.Service;
import org.granite.context.GraniteContext;
import org.granite.context.GraniteManager;
import org.granite.logging.Logger;
import org.granite.messaging.service.security.AbstractSecurityContext;
import org.granite.messaging.service.security.AbstractSecurityService;
import org.granite.messaging.service.security.SecurityServiceException;
import org.granite.osgi.service.GraniteSecurity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Instantiate
@Provides
public class OSGiSecurityService extends AbstractSecurityService {

    private static final Logger log = Logger.getLogger(OSGiSecurityService.class);

    Map<String, GraniteSecurity> securityMap = new HashMap<String, GraniteSecurity>();

    @Bind(aggregate = true, optional = true)
    public final void bindSecurity(final GraniteSecurity security) {
        synchronized (securityMap) {
            securityMap.put(security.getService(), security);
        }
    }

    @Unbind
    public final void unbindSecurity(final GraniteSecurity security) {
        synchronized (securityMap) {
            securityMap.remove(security.getService());
        }
    }

    @Override
    public void configure(Map<String, String> params) {
    }

    // Find the Security handler associated with this request
    private GraniteSecurity getSecurityHandler() {
            GraniteContext graniteContext = GraniteManager.getCurrentInstance();
            if (graniteContext != null) {
                Message message = graniteContext.getAMFContext().getRequest();
                List<Service> services = graniteContext.getServicesConfig().findServicesByMessageType(message.getClass().getName());
                Service service = null;
                for (Service ser : services) {
                    if (ser.findDestinationById(message.getDestination()) != null)
                        service = ser;
                }
                if (service != null) {
                    synchronized (securityMap) {
                        return securityMap.get(service.getId());
                    }
                }
            }
        return null;
    }

    @Override
    public void login(Object body) throws SecurityServiceException {
        GraniteSecurity graniteSecurity = getSecurityHandler();
        if (graniteSecurity != null) {
            String[] decoded = decodeBase64Credentials(body);
            graniteSecurity.login(decoded[0], decoded[1]);
        }
    }

    @Override
    public Object authorize(AbstractSecurityContext context) throws Exception {
        GraniteSecurity graniteSecurity = getSecurityHandler();
        startAuthorization(context);
        if (graniteSecurity != null) {
            graniteSecurity.authorize(context.getDestination(), context.getMessage());
        }
        return endAuthorization(context);
    }

    @Override
    public void logout() throws SecurityServiceException {
        GraniteSecurity graniteSecurity = getSecurityHandler();
        if (graniteSecurity != null) {
            graniteSecurity.logout();
        }
    }

    @Override
    public void handleSecurityException(SecurityServiceException e) {
    }
}
