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

import org.granite.logging.Logger;

import java.util.Map;

/**
 * @author Franck WOLFF
 */
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
}
