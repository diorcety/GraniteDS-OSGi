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
import org.granite.config.flex.Destination;
import org.granite.config.flex.ServicesConfig;
import org.granite.config.flex.SimpleService;
import org.granite.logging.Logger;

import java.util.HashMap;

@Component
@Provides
public class OSGiServiceWithAdapter extends SimpleService {

    private static final Logger log = Logger.getLogger(OSGiServiceWithAdapter.class);

    @Requires
    private ServicesConfig servicesConfig;

    @Requires(id = "defaultAdapter")
    private Adapter adapter;

    @ServiceProperty(name = "ID")
    private String ID;

    //
    private boolean started = false;

    //
    public OSGiServiceWithAdapter() {
        super(null, null, null, null, new HashMap<String, Adapter>(), new HashMap<String, Destination>());
    }

    @Property(name = "ID", mandatory = true)
    private void setId(String id) {
        this.id = id;
        this.ID = id;
    }

    @Property(name = "MESSAGETYPES", mandatory = true)
    private void setMessageTypes(String messageTypes) {
        this.messageTypes = messageTypes;
    }

    @Property(name = "CLASS", mandatory = false, value = "flex.messaging.services.RemotingService")
    private void setClass(String className) {
        this.className = className;
    }

    @Validate
    public void start() {
        log.debug("Start Service: " + toString());

        if (servicesConfig.findServiceById(id) == null) {
            servicesConfig.addService(this);

            // Clear destinations
            destinations.clear();

            this.defaultAdapter = adapter;
            started = true;
        } else {
            log.error("Service \"" + id + "\" already registered");
        }
    }

    @Invalidate
    public void stop() {
        log.debug("Stop Service: " + toString());
        if (servicesConfig != null) {
            servicesConfig.removeService(this.id);
            started = false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OSGiServiceWithAdapter that = (OSGiServiceWithAdapter) o;

        if (this != that) return false;

        return true;
    }

    @Override
    public String toString() {
        return "OSGiServiceWithAdapter{" +
                "ID=" + id +
                ", CLASS=" + className +
                ", MESSAGETYPES=" + messageTypes +
                ", DEFAULT_ADAPTER='" + adapter.getId() + '\'' +
                '}';
    }
}
