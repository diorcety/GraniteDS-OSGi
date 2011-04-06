package org.granite.config.flex;

import java.util.Map;

public interface ServiceComponent {
    public void addAdapter(Adapter adapter);


    public void addDestination(Destination
                                       destination);

    public String getId();

    public Adapter findAdapterById(String id);

    public Destination findDestinationById(String id);

    public Map<String, Adapter> getAdapters();

    public String getClassName();

    public Adapter getDefaultAdapter();

    public Map<String, Destination> getDestinations();

    public Destination removeDestination(String destinationId);

    public Adapter removeAdapter(String adapterId);

    public String getMessageTypes();

    public Service getService();
}
