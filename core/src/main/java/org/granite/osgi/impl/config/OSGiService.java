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

@Component(name = "org.granite.config.flex.Service")
@Provides
public class OSGiService extends SimpleService {

    private static final Logger LOG = Logger.getLogger(OSGiService.class);

    @Requires
    private ServicesConfig servicesConfig;

    @Property
    public Collection<String> ADAPTER_LIST;

    @Property
    public String DEFAULT_ADAPTER;

    //
    @ServiceController
    private boolean state = false;

    private boolean started = false;

    private int version = 0;

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
        if (this.ADAPTER_LIST != null && this.ADAPTER_LIST.contains(adapter.getId())) {
            this.adapters.put(adapter.getId(), adapter);

            if (this.DEFAULT_ADAPTER != null && this.DEFAULT_ADAPTER.equals(adapter.getId())) {
                this.defaultAdapter = adapter;
            }
            checkState();
        }
    }

    @Unbind
    private void unbindAdapter(Adapter adapter) {
        if (this.ADAPTER_LIST != null && this.ADAPTER_LIST.contains(adapter.getId())) {
            this.adapters.remove(adapter.getId());

            if (this.DEFAULT_ADAPTER != null && this.DEFAULT_ADAPTER.equals(adapter.getId())) {
                this.defaultAdapter = null;
            }
            checkState();
        }
    }

    private void checkState() {
        boolean new_state;
        if (started && (adapters == null || ADAPTER_LIST == null || adapters.size() == ADAPTER_LIST.size())) {
            new_state = true;
        } else {
            new_state = false;
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
        LOG.debug("Start Service: " + this.id);
        destinations.clear();
        servicesConfig.addService(this);
    }

    public void stop() {
        LOG.debug("Stop Service: " + this.id);
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
}
