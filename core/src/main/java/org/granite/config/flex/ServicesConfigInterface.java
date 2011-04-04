package org.granite.config.flex;

public interface ServicesConfigInterface {
    public void addFactory(Factory factory);

    public void addService(Service service);

    public void addChannel(Channel channel);

    public Factory removeFactory(String factoryId);

    public Service removeService(String serviceId);

    public Channel removeChannel(String channelId);
}
