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

package org.granite.gravity.osgi.adapters.ea.impl;

import flex.messaging.messages.AsyncMessage;
import flex.messaging.messages.Message;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import org.apache.felix.ipojo.annotations.Validate;
import org.granite.gravity.Channel;
import org.granite.gravity.MessageReceivingException;
import org.granite.gravity.osgi.adapters.ea.EAClient;
import org.granite.gravity.osgi.adapters.ea.EAConstants;
import org.granite.logging.Logger;
import org.granite.messaging.service.ServiceException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import javax.naming.NamingException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

@Component(name = "org.granite.gravity.osgi.adapters.ea.configuration")
@Provides
public class EAClientImpl implements EAClient {
    private static final Logger log = Logger.getLogger(EAClientImpl.class);

    @Property(name = "destination", mandatory = true)
    private String destination;

    @Property(name = "prefix", mandatory = false, value = "")
    private String prefix;


    @Requires
    private EventAdmin eventAdmin;

    private BundleContext bundleContext;

    private Map<String, EAConsumer> consumers = new HashMap<String, EAConsumer>();

    private EAClientImpl(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Validate
    private void start() throws NamingException, ServiceException {
        log.debug("Start EAClient: " + toString());
    }

    @Invalidate
    private void stop() throws NamingException, ServiceException {
        log.debug("Stop EAClient: " + toString());

        for (EAConsumer consumer : consumers.values())
            consumer.close();
    }

    @Override
    public String getDestination() {
        return destination;
    }

    public void subscribe(Channel channel, Message message) throws Exception {
        String subscriptionId = (String) message.getHeader(AsyncMessage.DESTINATION_CLIENT_ID_HEADER);
        String topic = (String) message.getHeader(AsyncMessage.SUBTOPIC_HEADER);

        synchronized (consumers) {
            EAConsumer consumer = new EAConsumer(channel, subscriptionId, normalize(prefix + topic));
            consumers.put(subscriptionId, consumer);

            channel.addSubscription(message.getDestination(), topic, subscriptionId, false);
        }
    }

    public void unsubscribe(Channel channel, Message message) throws Exception {
        String subscriptionId = (String) message.getHeader(AsyncMessage.DESTINATION_CLIENT_ID_HEADER);

        synchronized (consumers) {
            EAConsumer consumer = consumers.remove(subscriptionId);
            if (consumer != null)
                consumer.close();

            channel.removeSubscription(subscriptionId);
        }
    }

    private String normalize(String topic) {
        if (topic.startsWith("/"))
            return topic.substring(1);
        return topic;
    }

    @Override
    public void send(Message message) throws Exception {
        String topic = normalize(prefix + (String) message.getHeader(AsyncMessage.SUBTOPIC_HEADER));

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(EAConstants.DATA, message.getBody());
        Event reportGeneratedEvent = new Event(topic, properties);

        eventAdmin.sendEvent(reportGeneratedEvent);
    }

    private class EAConsumer implements EventHandler {
        private Channel channel;
        private String subscriptionId;
        private String topic;
        private ServiceRegistration serviceRegistration = null;

        EAConsumer(Channel channel, String subscriptionId, String topic) {
            this.channel = channel;
            this.subscriptionId = subscriptionId;
            this.topic = topic;
            Dictionary props = new Hashtable();
            props.put(EventConstants.EVENT_TOPIC, new String[]{topic});
            serviceRegistration = bundleContext.registerService(EventHandler.class.getName(), this, props);
            log.debug("Register \"" + topic + "\" on destination: " + destination);
        }

        void close() {
            log.debug("Unregister \"" + topic + "\" on destination: " + destination);
            serviceRegistration.unregister();
        }

        @Override
        public void handleEvent(Event event) {
            AsyncMessage dmsg = new AsyncMessage();
            try {
                dmsg.setDestination(destination);
                dmsg.setBody(event.getProperty(EAConstants.DATA));
                dmsg.setHeader(AsyncMessage.DESTINATION_CLIENT_ID_HEADER, subscriptionId);
                channel.receive(dmsg);
            } catch (MessageReceivingException e) {
                log.error("EventAdmin delivery error: ", e);
            }
        }
    }

    @Override
    public String toString() {
        return "EAClientImpl{" +
                "destination='" + destination + '\'' +
                '}';
    }
}
