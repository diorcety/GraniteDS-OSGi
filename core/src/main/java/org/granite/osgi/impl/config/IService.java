package org.granite.osgi.impl.config;

import org.granite.config.flex.Adapter;
import org.granite.config.flex.Destination;
import org.granite.config.flex.Service;

import java.util.Map;

public interface IService {
    public void addAdapter(Adapter adapter);

    public void addDestination(Destination destination);

    public String getId();

    public Adapter findAdapterById(String id);

    public Destination findDestinationById(String id);

    public Map<String, Adapter> getAdapters();

    public String getClassName();

    public Adapter getDefaultAdapter();

    public Map<String, Destination> getDestinations();

    public void removeDestination(String destinationId);

    public void removeAdapter(String adapterId);

    public String getMessageTypes();

    public Service getService();
}
