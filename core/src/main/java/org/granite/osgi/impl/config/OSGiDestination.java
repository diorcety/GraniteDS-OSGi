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

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.ServiceController;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.flex.Adapter;
import org.granite.config.flex.Channel;
import org.granite.config.flex.Factory;
import org.granite.config.flex.Service;
import org.granite.config.flex.SimpleDestination;
import org.granite.logging.Logger;
import org.granite.osgi.service.GraniteDestination;
import org.granite.util.XMap;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

@Component(name = "org.granite.config.flex.Destination")
@Provides
public class OSGiDestination extends SimpleDestination {

    private static final Logger log = Logger.getLogger(OSGiDestination.class);

    @Property(name = "SERVICE", mandatory = true)
    public String SERVICE;

    @Property(name = "CHANNELS", mandatory = true)
    public String[] CHANNELS;

    @Property(name = "ADAPTER", mandatory = false)
    public String ADAPTER;

    //
    @ServiceController
    private boolean state = false;

    private boolean started = false;

    private Service service;

    private Factory factory;

    private Map<String, Service> _services = new Hashtable<String, Service>();
    private Map<String, Adapter> _adapters = new Hashtable<String, Adapter>();
    private Map<String, Factory> _factories = new Hashtable<String, Factory>();
    private Map<String, Channel> _channels = new Hashtable<String, Channel>();

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
    private void setScope(GraniteDestination.SCOPE scope) {
        this.properties.put("scope", scope.toString());
    }

    @Bind(aggregate = true, optional = true)
    private void bindService(Service service) {
        _services.put(service.getId(), service);
        checkState();

    }

    @Unbind
    private void unbindService(Service service) {
        _services.remove(service.getId());
        checkState();

    }

    @Bind(aggregate = true, optional = true)
    private void bindAdapter(Adapter adapter) {
        _adapters.put(adapter.getId(), adapter);
        checkState();

    }

    @Unbind
    private void unbindAdapter(Adapter adapter) {
        _adapters.remove(adapter.getId());
        checkState();

    }

    @Bind(aggregate = true, optional = true)
    private void bindChannel(Channel channel) {
        _channels.put(channel.getId(), channel);
        checkState();

    }

    @Unbind
    private void unbindChannel(Channel channel) {
        _channels.remove(channel.getId());
        checkState();
    }

    @Bind(aggregate = true, optional = true)
    private void bindFactory(Factory factory) {
        _factories.put(factory.getId(), factory);
        checkState();

    }

    @Unbind
    private void unbindFactory(Factory factory) {
        _factories.remove(factory.getId());
        checkState();

    }

    private void checkState() {
        boolean new_state = false;
        if (started) {
            if ((_services.containsKey(SERVICE)) &&
                    (ADAPTER == null || _adapters.containsKey(ADAPTER)) &&
                    (!properties.containsKey("factory") || _factories.containsKey(properties.get("factory")))) {
                for (String channelId : CHANNELS) {
                    if (_channels.containsKey(channelId)) {
                        new_state = true;
                        break;
                    }
                }

            }
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

        // CHANNELS
        this.channelRefs.clear();
        for (String channelId : CHANNELS) {
            Channel channel = _channels.get(channelId);
            if (channel != null)
                this.channelRefs.add(channelId);
        }

        // SERVICE
        this.service = _services.get(SERVICE);

        // ADAPTER
        this.adapter = null;
        if (ADAPTER != null)
            this.adapter = _adapters.get(ADAPTER);
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
                ", FACTORY='" + properties.get("factory")  + '\'' +
                ", SCOPE='" + properties.get("scope")  + '\'' +
                '}';
    }
}
