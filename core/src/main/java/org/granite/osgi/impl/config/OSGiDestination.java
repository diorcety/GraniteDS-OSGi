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
import org.granite.config.flex.Channel;
import org.granite.config.flex.Service;
import org.granite.config.flex.SimpleDestination;
import org.granite.logging.Logger;
import org.granite.util.XMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Component(name = "org.granite.config.flex.Destination")
@Provides
public class OSGiDestination extends SimpleDestination {

    private static final Logger log = Logger.getLogger(OSGiDestination.class);

    @Property(name = "SERVICE", mandatory = true)
    public String SERVICE;

    @Property(name = "CHANNELS", mandatory = true)
    public Collection<String> CHANNELS;

    @Property(name = "ADAPTER", mandatory = false)
    public String ADAPTER;

    //
    private boolean state = false;

    private boolean started = false;

    private Service service;

    //
    protected OSGiDestination() {
        super(null, new ArrayList<String>(), new XMap(), new ArrayList<String>(), null, null);
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

    @Property(name = "ID", mandatory = true)
    private void setId(String id) {
        this.id = id;
    }

    @Property(name = "FACTORY", mandatory = false)
    private void setFactory(String factory) {
        this.properties.put("factory", factory);
    }

    @Property(name = "SCOPE", mandatory = false)
    private void setScope(String scope) {
        this.properties.put("scope", scope);
    }

    @Bind(aggregate = true, optional = true)
    private void bindService(Service service) {
        if (this.SERVICE.equals(service.getId())) {
            this.service = service;
            checkState();
        }
    }

    @Unbind
    private void unbindService(Service service) {
        if (this.SERVICE.equals(service.getId())) {
            this.service = null;
            checkState();
        }
    }

    @Bind(aggregate = true, optional = true)
    private void bindAdapter(Adapter adapter) {
        if (this.ADAPTER != null && this.ADAPTER.equals(adapter.getId())) {
            this.adapter = adapter;
            checkState();
        }
    }

    @Unbind
    private void unbindAdapter(Adapter adapter) {
        if (this.ADAPTER != null && this.ADAPTER.equals(adapter.getId())) {
            this.adapter = null;
            checkState();
        }
    }

    @Bind(aggregate = true, optional = true)
    private void bindChannel(Channel channel) {
        if (this.CHANNELS.contains(channel.getId())) {
            this.channelRefs.add(channel.getId());
            checkState();
        }
    }

    @Unbind
    private void unbindChannel(Channel channel) {
        if (this.CHANNELS.contains(channel.getId())) {
            this.channelRefs.remove(channel.getId());
            checkState();
        }
    }

    private void checkState() {
        boolean new_state = false;

        // Check state
        if (started && service != null && this.channelRefs.size() > 0) {
            if (ADAPTER == null || adapter != null)
                new_state = true;
        }
        // Update sate
        if (new_state != this.state) {
            if (new_state)
                start();
            else
                stop();

            this.state = new_state;
        }
    }

    public void start() {
        log.debug("Start Destination: " + toString());
        if (this.adapter == null)
            this.adapter = service.getDefaultAdapter();
        service.addDestination(this);
    }

    public void stop() {
        log.debug("Stop Destination: " + toString());
        if (service != null) {
            service.removeDestination(this.id);
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
                ", ADAPTER='" + ADAPTER + '\'' +
                ", CHANNELS=" + CHANNELS +
                ", SERVICE='" + SERVICE + '\'' +
                '}';
    }
}
