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

package org.granite.osgi.impl.config.composite;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;
import org.granite.config.flex.ServicesConfig;
import org.granite.logging.Logger;
import org.granite.osgi.GraniteConstants;

import java.util.Dictionary;
import java.util.Hashtable;

@Component(name = GraniteConstants.SERVICE)
public class OSGiService {
    private static final Logger log = Logger.getLogger(OSGiService.class);

    @Property(name = "id", mandatory = true)
    private String id;

    @Property(name = "messageTypes", value = "flex.messaging.messages.RemotingMessage", mandatory = false)
    private String messageTypes;

    @Property(name = "class", value = "flex.messaging.services.RemotingService", mandatory = false)
    private String clazz;

    @Property(name = "defaultAdapter", mandatory = false)
    private String defaultAdapter;

    @Requires(from = "org.granite.osgi.impl.config.composite.OSGiServiceSimple")
    private Factory simpleBuilder;

    @Requires(from = "org.granite.osgi.impl.config.composite.OSGiServiceAdapter")
    private Factory adapterBuilder;

    private ComponentInstance instance;

    @Validate
    public void start() {
        try {
            if (defaultAdapter == null) {
                Dictionary properties = new Hashtable();
                properties.put("id", id);
                properties.put("messageTypes", messageTypes);
                properties.put("class", clazz);
                instance = simpleBuilder.createComponentInstance(properties);
            } else {
                Dictionary filters = new Hashtable();
                filters.put("defaultAdapter", "(ID=" + defaultAdapter + ")");

                Dictionary properties = new Hashtable();
                properties.put("id", id);
                properties.put("messageTypes", messageTypes);
                properties.put("class", clazz);
                properties.put("requires.filters", filters);
                instance = adapterBuilder.createComponentInstance(properties);
            }
        } catch (Exception e) {
            log.error(e, "Invalid Service configuration");
        }
    }

    @Invalidate
    public void stop() {
        instance.dispose();
    }
}
