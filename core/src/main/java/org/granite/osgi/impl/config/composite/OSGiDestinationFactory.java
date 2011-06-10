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

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;
import org.granite.config.flex.Factory;
import org.granite.config.flex.Service;
import org.granite.config.flex.SimpleDestination;
import org.granite.logging.Logger;
import org.granite.util.XMap;

import java.util.ArrayList;

@Component
@Provides
public class OSGiDestinationFactory extends SimpleDestination {

    private static final Logger log = Logger.getLogger(OSGiDestinationFactory.class);

    @ServiceProperty(name = "ID")
    private String ID;

    //
    private boolean started = false;

    @Requires(id = "service", proxy = false)
    private Service service;

    @Requires(id = "factory", proxy = false)
    private Factory factory;

    //
    protected OSGiDestinationFactory() {
        super(null, new ArrayList<String>(), new XMap(), new ArrayList<String>(), null, null);
    }


    @Property(name = "id", mandatory = true)
    private void setId(String id) {
        this.id = id;
        this.ID = id;
    }

    @Property(name = "scope", mandatory = false)
    private void setScope(String scope) {
        this.properties.put("scope", scope);
    }

    @Validate
    public void start() {
        log.debug("Start OSGiDestinationFactory: " + toString());

        if (service.findDestinationById(id) == null) {
            this.properties.put("factory", factory.getId());

            service.addDestination(this);
            started = true;
        } else {
            log.error("Destination \"" + id + "\" already registered");
        }
    }

    @Invalidate
    public void stop() {
        log.debug("Stop OSGiDestinationFactory: " + toString());
        if (started) {
            service.removeDestination(id);
            started = false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OSGiDestinationFactory that = (OSGiDestinationFactory) o;

        if (this != that) return false;

        return true;
    }

    @Override
    public String toString() {
        return "Destination{" +
                "id='" + id + '\'' +
                ", service='" + service.getId() + '\'' +
                ", factory='" + factory.getId() + '\'' +
                ", scope='" + properties.get("scope") + '\'' +
                '}';
    }
}
