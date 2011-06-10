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
import org.apache.felix.ipojo.annotations.Validate;
import org.granite.logging.Logger;

import java.util.Dictionary;
import java.util.Hashtable;

@Component(name = "org.granite.config.flex.Destination")
public class OSGiDestination {
    private static final Logger log = Logger.getLogger(OSGiDestination.class);

    @Property(name = "id", mandatory = true)
    String id;

    @Property(name = "service", mandatory = true)
    String service;

    @Property(name = "factory", mandatory = false)
    String factory;

    @Property(name = "scope", mandatory = false)
    String scope;

    @Property(name = "adapter", mandatory = false)
    String adapter;

    @Requires(from = "org.granite.osgi.impl.config.composite.OSGiDestinationSimple")
    private Factory simpleBuilder;

    @Requires(from = "org.granite.osgi.impl.config.composite.OSGiDestinationAdapter")
    private Factory adapterBuilder;

    @Requires(from = "org.granite.osgi.impl.config.composite.OSGiDestinationFactory")
    private Factory factoryBuilder;

    private ComponentInstance instance;

    @Validate
    public void start() {
        try {
            if (adapter != null) {
                Dictionary filters = new Hashtable();
                filters.put("service", "(ID=" + service + ")");
                filters.put("adapter", "(ID=" + adapter + ")");
                Dictionary properties = new Hashtable();
                properties.put("id", id);
                properties.put("requires.filters", filters);
                instance = adapterBuilder.createComponentInstance(properties);
            } else if (factory != null) {
                Dictionary filters = new Hashtable();
                filters.put("service", "(ID=" + service + ")");
                filters.put("factory", "(ID=" + factory + ")");
                Dictionary properties = new Hashtable();
                properties.put("id", id);
                if (scope != null)
                    properties.put("scope", scope.toString());
                properties.put("requires.filters", filters);
                instance = factoryBuilder.createComponentInstance(properties);
            } else {
                Dictionary filters = new Hashtable();
                filters.put("service", "(ID=" + service + ")");

                Dictionary properties = new Hashtable();
                properties.put("id", id);
                properties.put("requires.filters", filters);
                instance = simpleBuilder.createComponentInstance(properties);
            }
        } catch (Exception e) {
            log.error(e, "Invalid Destination configuration");
        }
    }

    @Invalidate
    public void stop() {
        instance.dispose();
    }
}
