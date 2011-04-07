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

import flex.messaging.messages.RemotingMessage;

import org.granite.config.api.Configuration;
import org.granite.logging.Logger;
import org.granite.messaging.service.annotations.RemoteDestination;
import org.granite.scan.ScannedItem;
import org.granite.scan.ScannedItemHandler;
import org.granite.scan.Scanner;
import org.granite.scan.ScannerFactory;
import org.granite.util.ClassUtil;
import org.granite.util.XMap;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Franck WOLFF
 */
public class ServicesConfig implements
        ScannedItemHandler, ServicesConfigComponent {

    ///////////////////////////////////////////////////////////////////////////
    // Fields.

    private static final Logger LOG = Logger.getLogger(ServicesConfig.class);
    private static final String SERVICES_CONFIG_PROPERTIES = "META-INF/services-config.properties";

    protected Map<String, Service> services = new HashMap<String, Service>();
    protected Map<String, Channel> channels = new HashMap<String, Channel>();
    protected Map<String, Factory> factories = new HashMap<String, Factory>();


    ///////////////////////////////////////////////////////////////////////////
    // Classpath scan initialization.

    private void scanConfig(String serviceConfigProperties,
                            List<ScannedItemHandler> handlers) {
        Scanner scanner = ScannerFactory.createScanner(this,
                                                       serviceConfigProperties != null ? serviceConfigProperties : SERVICES_CONFIG_PROPERTIES);
        scanner.addHandlers(handlers);
        try {
            scanner.scan();
        } catch (Exception e) {
            LOG.error(e, "Could not scan classpath for configuration");
        }
    }

    public boolean handleMarkerItem(ScannedItem item) {
        return false;
    }

    public void handleScannedItem(ScannedItem item) {
        if ("class".equals(item.getExtension()) && item.getName().indexOf(
                '$') == -1) {
            try {
                handleClass(item.loadAsClass());
            } catch (Throwable t) {
                LOG.error(t, "Could not load class: %s", item);
            }
        }
    }

    public void handleClass(Class<?> clazz) {
        RemoteDestination anno = clazz.getAnnotation(RemoteDestination.class);
        if (anno != null && !("".equals(anno.id()))) {
            XMap props = new XMap("properties");

            // Owning service.
            Service service = null;
            if (anno.service().length() > 0)
                service = this.services.get(anno.service());
            else if (this.services.size() > 0) {
                // Lookup remoting service
                int count = 0;
                for (Service s : this.services.values()) {
                    if (RemotingMessage.class.getName().equals(
                            s.getMessageTypes())) {
                        service = s;
                        count++;
                    }
                }
                if (count == 1 && service != null)
                    LOG.info(
                            "Service " + service.getId() + " selected for destination in class: " + clazz.getName());
                else
                    service = null;
            }
            if (service == null)
                throw new RuntimeException(
                        "No service found for destination in class: " + clazz.getName());

            // Channel reference.
            List<String> channelIds = null;
            if (anno.channels().length > 0)
                channelIds = Arrays.asList(anno.channels());
            else if (anno.channel().length() > 0)
                channelIds = Collections.singletonList(anno.channel());
            else if (this.channels.size() == 1) {
                channelIds = new ArrayList<String>(this.channels.keySet());
                LOG.info("Channel " + channelIds.get(
                        0) + " selected for destination in class: " + clazz.getName());
            } else {
                LOG.warn(
                        "No (or ambiguous) channel definition found for destination in class: " + clazz.getName());
                channelIds = Collections.emptyList();
            }

            // Factory reference.
            String factoryId = null;
            if (anno.factory().length() > 0)
                factoryId = anno.factory();
            else if (this.factories.isEmpty()) {
                props.put("scope", anno.scope());
                props.put("source", clazz.getName());
                LOG.info(
                        "Default POJO factory selected for destination in class: " + clazz.getName() + " with scope: " + anno.scope());
            } else if (this.factories.size() == 1) {
                factoryId = this.factories.keySet().iterator().next();
                LOG.info(
                        "Factory " + factoryId + " selected for destination in class: " + clazz.getName());
            } else
                throw new RuntimeException(
                        "No (or ambiguous) factory definition found for destination in class: " + clazz.getName());

            if (factoryId != null)
                props.put("factory", factoryId);
            if (!(anno.source().equals("")))
                props.put("source", anno.source());

            // Security roles.
            List<String> roles = null;
            if (anno.securityRoles().length > 0) {
                roles = new ArrayList<String>(anno.securityRoles().length);
                for (String role : anno.securityRoles())
                    roles.add(role);
            }

            Destination destination = new Destination(anno.id(), channelIds,
                                                      props, roles, null,
                                                      clazz);

            service.getDestinations().put(destination.getId(), destination);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Static ServicesConfig loaders.

    public ServicesConfig(InputStream customConfigIs,
                          Configuration configuration,
                          boolean scan) throws IOException, SAXException {
        if (customConfigIs != null)
            loadConfig(customConfigIs);

        if (scan)
            scan(configuration);
    }

    public ServicesConfig(Map<String, Service> services, Map<String,
            Channel> channels, Map<String, Factory> factories) {
        this.services = services;
        this.channels = channels;
        this.factories = factories;
    }

    public ServicesConfig()
    {

    }

    public void scan(Configuration configuration) {
        List<ScannedItemHandler> handlers = new ArrayList<ScannedItemHandler>();
        for (Factory factory : factories.values()) {
            try {
                Class<?> clazz = ClassUtil.forName(factory.getClassName());
                Method method = clazz.getMethod("getScannedItemHandler");
                if ((Modifier.STATIC & method.getModifiers()) != 0 && method.getParameterTypes().length == 0) {
                    ScannedItemHandler handler = (ScannedItemHandler) method.invoke(
                            null);
                    handlers.add(handler);
                } else
                    LOG.warn(
                            "Factory class %s contains an unexpected signature for method: %s",
                            factory.getClassName(), method);
            } catch (NoSuchMethodException e) {
                // ignore
            } catch (ClassNotFoundException e) {
                LOG.error(e, "Could not load factory class: %s",
                          factory.getClassName());
            } catch (Exception e) {
                LOG.error(e,
                          "Error while calling %s.getScannedItemHandler() method",
                          factory.getClassName());
            }
        }
        scanConfig(
                configuration != null ? configuration.getFlexServicesConfigProperties() : null,
                handlers);
    }

    private void loadConfig(
            InputStream configIs) throws IOException, SAXException {
        XMap doc = new XMap(configIs);
        StaticHelper.getServicesConfig(doc);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Services.

    public Service findServiceById(String id) {
        return services.get(id);
    }

    public List<Service> findServicesByMessageType(String messageType) {
        List<Service> services = new ArrayList<Service>();
        for (Service service : this.services.values()) {
            if (messageType.equals(service.getMessageTypes()))
                services.add(service);
        }
        return services;
    }

    public Map<String, Service> getServices() {
        return services;
    }

    public void addService(Service service) {
        services.put(service.getId(), service);
    }

    public Service removeService(String serviceId) {
        return services.remove(serviceId);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Channels.

    public Channel findChannelById(String id) {
        return channels.get(id);
    }

    public Map<String, Channel> getChannels() {
        return channels;
    }

    public void addChannel(Channel channel) {
        channels.put(channel.getId(), channel);
    }

    public Channel removeChannel(String channelId) {
        return channels.remove(channelId);
    }

    @Override
    public ServicesConfig getServicesConfig() {
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Factories.

    public Factory findFactoryById(String id) {
        return factories.get(id);
    }

    public Map<String, Factory> getFactories() {
        return factories;
    }

    public void addFactory(Factory factory) {
        factories.put(factory.getId(), factory);
    }

    public Factory removeFactory(String factoryId) {
        return factories.remove(factoryId);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Destinations.

    public Destination findDestinationById(String messageType, String id) {
        for (Service service : services.values()) {
            if (messageType == null || messageType.equals(
                    service.getMessageTypes())) {
                Destination destination = service.findDestinationById(id);
                if (destination != null)
                    return destination;
            }
        }
        return null;
    }

    public List<Destination> findDestinationsByMessageType(String messageType) {
        List<Destination> destinations = new ArrayList<Destination>();
        for (Service service : services.values()) {
            if (messageType.equals(service.getMessageTypes()))
                destinations.addAll(service.getDestinations().values());
        }
        return destinations;
    }

    @Override
    public String toString() {
        return "ServicesConfig{" +
                "channels=" + channels +
                ", services=" + services +
                ", factories=" + factories +
                '}';
    }
}
