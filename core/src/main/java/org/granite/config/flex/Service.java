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

package org.granite.config.flex;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceController;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.logging.Logger;
import org.granite.util.XMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Franck WOLFF
 */
@Component
@Provides
public class Service implements ServiceComponent {

    private static final Logger LOG = Logger.getLogger(Service.class);

    protected String id;
    protected String className;
    protected String messageTypes;
    protected Map<String, Adapter> adapters;
    protected Adapter defaultAdapter;
    protected Map<String, Destination> destinations;


    public Service(String id, String className, String messageTypes,
                   Adapter defaultAdapter, Map<String, Adapter> adapters,
                   Map<String, Destination> destinations) {
        this.id = id;
        this.className = className;
        this.messageTypes = messageTypes;
        this.defaultAdapter = defaultAdapter;
        this.adapters = adapters;
        this.destinations = destinations;
    }

    public String getId() {
        return id;
    }

    public String getClassName() {
        return className;
    }

    public String getMessageTypes() {
        return messageTypes;
    }

    @Override
    public Service getService() {
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Destinations.

    public Destination findDestinationById(String id) {
        return destinations.get(id);
    }

    public Map<String, Destination> getDestinations() {
        return destinations;
    }

    public void addDestination(Destination destination) {
        destinations.put(destination.getId(), destination);
    }

    public Destination removeDestination(String destinationId) {
        return destinations.remove(destinationId);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Adapters.

    public Adapter findAdapterById(String id) {
        return adapters.get(id);
    }

    public Map<String, Adapter> getAdapters() {
        return adapters;
    }

    public Adapter getDefaultAdapter() {
        return defaultAdapter;
    }

    public void addAdapter(Adapter adapter) {
        adapters.put(adapter.getId(), adapter);
    }

    public Adapter removeAdapter(String adapterId) {
        return adapters.remove(adapterId);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Static helper.

    public static Service forElement(XMap element) {
        String id = element.get("@id");
        String className = element.get("@class");
        String messageTypes = element.get("@messageTypes");

        Adapter defaultAdapter = null;
        Map<String, Adapter> adaptersMap = new HashMap<String, Adapter>();
        for (XMap adapter : element.getAll("adapters/adapter-definition")) {
            Adapter ad = Adapter.forElement(adapter);
            if (Boolean.TRUE.toString().equals(adapter.get("@default")))
                defaultAdapter = ad;
            adaptersMap.put(ad.getId(), ad);
        }

        Map<String, Destination> destinations = new HashMap<String, Destination>();
        for (XMap destinationElt : element.getAll("destination")) {
            Destination destination = Destination.forElement(destinationElt,
                                                             defaultAdapter,
                                                             adaptersMap);
            destinations.put(destination.getId(), destination);
        }

        return new Service(id, className, messageTypes, defaultAdapter,
                           adaptersMap, destinations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Service service = (Service) o;

        if (id != null ? !id.equals(service.id) : service.id != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Service{" +
                "adapters=" + adapters +
                ", id='" + id + '\'' +
                ", className='" + className + '\'' +
                ", messageTypes='" + messageTypes + '\'' +
                ", defaultAdapter=" + defaultAdapter +
                ", destinations=" + destinations +
                '}';
    }

    ///////////////////////////////////////////////////////////////////////////
    // OSGi

    @Requires
    private ServicesConfigComponent servicesConfig;

    @Property
    public Collection<String> ADAPTER_LIST;

    @Property
    public String DEFAULT_ADAPTER;

    //
    @ServiceController
    private boolean state = false;

    private boolean started = false;

    public Service() {
        this.id = null;
        this.className = null;
        this.messageTypes = null;
        this.defaultAdapter = null;
        this.adapters = new HashMap<String, Adapter>();
        this.destinations = new HashMap<String, Destination>();
    }

    @Property(name = "ID", mandatory = true)
    private void setId(String id) {
        this.id = id;
    }

    @Property(name = "MESSAGETYPES", mandatory = false,
              value = "flex.messaging.messages.RemotingMessage")
    private void setMessageTypes(String messageTypes) {
        this.messageTypes = messageTypes;
    }

    @Property(name = "CLASS", mandatory = false,
              value = "flex.messaging.services.RemotingService")
    private void setClass(String className) {
        this.className = className;
    }

    @Bind(aggregate = true, optional = true)
    private void bindAdapter(AdapterComponent adapter) {
        if (this.ADAPTER_LIST != null && this.ADAPTER_LIST.contains(
                adapter.getId())) {
            this.adapters.put(adapter.getId(), (Adapter) adapter);   //HACK

            if (this.DEFAULT_ADAPTER != null &&
                    this.DEFAULT_ADAPTER.equals(adapter.getId())) {
                this.defaultAdapter = (Adapter) adapter;    //HACK
            }
            checkState();
        }
    }

    @Unbind
    private void unbindAdapter(AdapterComponent adapter) {
        if (this.ADAPTER_LIST != null && this.ADAPTER_LIST.contains(
                adapter.getId())) {
            this.adapters.remove(adapter.getId());

            if (this.DEFAULT_ADAPTER != null &&
                    this.DEFAULT_ADAPTER.equals(adapter.getId())) {
                this.defaultAdapter = null;
            }
            checkState();
        }
    }

    private void checkState() {
        boolean new_state;
        if (started && (adapters == null || ADAPTER_LIST == null
                || adapters.size() == ADAPTER_LIST.size())) {
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

    @Validate
    public void starting() {
        started = true;
        checkState();
    }

    public void start() {
        LOG.debug("Start Service:" + this.id);
        getDestinations().clear();
        servicesConfig.addService(this);
    }

    @Invalidate
    public void stopping() {
        if (this.state) {
            stop();
            this.state = false;
        }
        started = false;
    }

    public void stop() {
        LOG.debug("Stop Service:" + this.id);
        if (servicesConfig != null) {
            servicesConfig.removeService(this.id);
        }
    }
}
