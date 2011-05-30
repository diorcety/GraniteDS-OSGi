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

package org.granite.osgi.impl.config;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceController;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.flex.Adapter;
import org.granite.config.flex.Service;
import org.granite.config.flex.SimpleDestination;
import org.granite.logging.Logger;
import org.granite.util.XMap;

import java.util.ArrayList;

@Component
@Provides
public class OSGiDestination extends SimpleDestination {

    private static final Logger log = Logger.getLogger(OSGiDestination.class);

    @ServiceProperty(name = "ID")
    private String ID;

    //
    private boolean started = false;

    @Requires(id="service")
    private Service service;

    //
    protected OSGiDestination() {
        super(null, new ArrayList<String>(), new XMap(), new ArrayList<String>(), null, null);
    }


    @Property(name = "ID", mandatory = true)
    private void setId(String id) {
        this.id = id;
        this.ID = id;
    }

    @Override
    public Adapter getAdapter() {
        return service.getDefaultAdapter();
    }

    @Validate
    public void start() {
        log.debug("Start Destination: " + toString());

        if (service.findDestinationById(id) == null) {
            service.addDestination(this);
            started = true;
        } else {
            log.error("Destination \"" + id + "\" already registered");
        }
    }

    @Invalidate
    public void stop() {
        log.debug("Stop Destination: " + toString());
        if (started) {
            service.removeDestination(id);
            started = false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OSGiDestination that = (OSGiDestination) o;

        if (this != that) return false;

        return true;
    }

    @Override
    public String toString() {
        return "OSGiDestination{" +
                "ID='" + id + '\'' +
                ", SERVICE='" + service.getId() + '\'' +
                '}';
    }
}
