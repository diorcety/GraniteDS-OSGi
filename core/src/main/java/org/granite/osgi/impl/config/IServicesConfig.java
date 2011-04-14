package org.granite.osgi.impl.config;

import org.granite.config.api.Configuration;
import org.granite.config.flex.Channel;
import org.granite.config.flex.Destination;
import org.granite.config.flex.Factory;
import org.granite.config.flex.Service;

import java.util.List;

public interface IServicesConfig {
    public void addFactory(Factory factory);

    public void addService(Service service);

    public void addChannel(Channel channel);

    public void removeFactory(String factoryId);

    public void removeService(String serviceId);

    public void removeChannel(String channelId);

    public Destination findDestinationById(String messageType, String id);

    public List<Destination> findDestinationsByMessageType(String messageType);

    public Service findServiceById(String id);

    public List<Service> findServicesByMessageType(String messageType);

    public Channel findChannelById(String id);

    public Factory findFactoryById(String id);

    public void scan(Configuration configuration);

    public void handleClass(Class<?> clazz);
}
