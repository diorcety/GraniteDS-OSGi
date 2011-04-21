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

import org.apache.felix.ipojo.annotations.*;

import org.granite.config.flex.Adapter;
import org.granite.config.flex.Destination;
import org.granite.config.flex.ServicesConfig;
import org.granite.config.flex.SimpleService;
import org.granite.logging.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

@Component(name = "org.granite.config.flex.Service")
@Provides
public class OSGiService extends SimpleService {

    private static final Logger LOG = Logger.getLogger(OSGiService.class);

    @Requires
    private ServicesConfig servicesConfig;

    @Property
    public Collection<String> ADAPTERS;

    @Property
    public String DEFAULT_ADAPTER;

    //
    @ServiceController
    private boolean state = false;
    private boolean started = false;
    private int version = 0;

    private Map<String, Adapter> _adapters = new Hashtable<String, Adapter>();

    //
    public OSGiService() {
        super(null, null, null, null, new HashMap<String, Adapter>(), new HashMap<String, Destination>());
    }

    @Validate
    public void starting() {
        started = true;
        checkState();
    }

    @Invalidate
    public void stopping() {
        started = false;
        checkState();
    }

    @Updated
    public void updated() {
        if (started) {
            started = false;
            checkState();
        }
        version++;
        started = true;
        checkState();
    }


    @Property(name = "ID", mandatory = true)
    private void setId(String id) {
        this.id = id;
    }

    @Property(name = "MESSAGETYPES", mandatory = false, value = "flex.messaging.messages.RemotingMessage")
    private void setMessageTypes(String messageTypes) {
        this.messageTypes = messageTypes;
    }

    @Property(name = "CLASS", mandatory = false, value = "flex.messaging.services.RemotingService")
    private void setClass(String className) {
        this.className = className;
    }

    @Bind(aggregate = true, optional = true)
    private void bindAdapter(Adapter adapter) {
        _adapters.put(adapter.getId(), adapter);
        if (!state) {
            checkState();
        }
    }

    @Unbind
    private void unbindAdapter(Adapter adapter) {
        _adapters.remove(adapter.getId());
        if (state) {
            checkState();
        }
    }

    private void checkState() {
        boolean new_state = false;
        if (started) {
            if (DEFAULT_ADAPTER == null || ADAPTERS.contains(DEFAULT_ADAPTER)) {
                new_state = true;
                if (ADAPTERS != null) {
                    for (String adapterId : ADAPTERS) {
                        if (!_adapters.containsKey(adapterId)) {
                            new_state = false;
                            break;
                        }

                    }
                }
            }
        }
        if (new_state != this.state) {
            if (new_state)
                start();
            else
                stop();

            this.state = new_state;
        }
    }

    public void start() {
        LOG.debug("Start Service: " + toString());

        // Clear destinations
        destinations.clear();

        // ADAPTERS
        adapters.clear();
        if (ADAPTERS != null) {
            for (String adapterId : ADAPTERS) {
                adapters.put(adapterId, _adapters.get(adapterId));
            }
        }

        // DEFAULT ADAPTER
        this.defaultAdapter = null;
        if (DEFAULT_ADAPTER != null)
            this.defaultAdapter = adapters.get(DEFAULT_ADAPTER);

        servicesConfig.addService(this);
    }

    public void stop() {
        LOG.debug("Stop Service: " + toString());
        if (servicesConfig != null) {
            servicesConfig.removeService(this.id);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OSGiService that = (OSGiService) o;

        if (this != that || version != that.version) return false;

        return true;
    }

    @Override
    public String toString() {
        return "OSGiService{" +
                "ID=" + id +
                ", CLASS=" + className +
                ", MESSAGETYPES=" + messageTypes +
                ", ADAPTERS=" + ADAPTERS +
                ", DEFAULT_ADAPTER='" + DEFAULT_ADAPTER + '\'' +
                '}';
    }
}
