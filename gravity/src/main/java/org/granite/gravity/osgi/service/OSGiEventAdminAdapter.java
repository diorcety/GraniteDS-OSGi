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

package org.granite.gravity.osgi.service;

import flex.messaging.messages.AcknowledgeMessage;
import flex.messaging.messages.AsyncMessage;
import flex.messaging.messages.CommandMessage;
import flex.messaging.messages.ErrorMessage;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.felix.ipojo.handlers.event.publisher.Publisher;

import org.granite.gravity.Channel;
import org.granite.gravity.adapters.ServiceAdapter;
import org.granite.gravity.adapters.TopicId;
import org.granite.logging.Logger;
import org.granite.osgi.service.GraniteAdapter;

import org.osgi.service.event.Event;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

@Component(name = "org.granite.gravity.osgi.OSGiEventAdminAdapter")
@Provides
public class OSGiEventAdminAdapter extends ServiceAdapter implements GraniteAdapter {

    private static final Logger log = Logger.getLogger(OSGiEventAdminAdapter.class);

    private final OSGiTopic rootTopic = new OSGiTopic("/", this);
    private transient ConcurrentHashMap<String, TopicId> _topicIdCache = new ConcurrentHashMap<String, TopicId>();
    private transient ConcurrentHashMap<String, Channel> topicChannels = new ConcurrentHashMap<String, Channel>();

    @Property(name = "ID", mandatory = true)
    private String ID;

    private boolean noLocal = false;

    @org.apache.felix.ipojo.handlers.event.Publisher(
            name = "GravityPublisher"
    )

    private Publisher publisher;

    @Validate
    public void starting() {
        log.debug("Start OSGiEventAdminAdapter \"" + ID + "\"");
    }

    @Invalidate
    public void stopping() {
        log.debug("Stop OSGiEventAdminAdapter \"" + ID + "\"");
    }

    public OSGiTopic getTopic(TopicId id) {
        return rootTopic.getChild(id);
    }

    public OSGiTopic getTopic(String id) {
        TopicId cid = getTopicId(id);
        if (cid.depth() == 0)
            return null;
        return rootTopic.getChild(cid);
    }

    public OSGiTopic getTopic(String id, boolean create) {
        synchronized (this) {
            OSGiTopic topic = getTopic(id);

            if (topic == null && create) {
                topic = new OSGiTopic(id, this);
                rootTopic.addChild(topic);
                log.debug("New Topic: %s", topic);
            }
            return topic;
        }
    }

    public TopicId getTopicId(String id) {
        TopicId tid = _topicIdCache.get(id);
        if (tid == null) {
            tid = new TopicId(id);
            _topicIdCache.put(id, tid);
        }
        return tid;
    }

    public boolean hasTopic(String id) {
        TopicId cid = getTopicId(id);
        return rootTopic.getChild(cid) != null;
    }

    @Override
    public Object invoke(Channel fromChannel, AsyncMessage message) {
        String topicId = TopicId.normalize(((String) message.getHeader(AsyncMessage.SUBTOPIC_HEADER)));

        AsyncMessage reply = null;

        if (getSecurityPolicy().canPublish(fromChannel, topicId, message)) {
            TopicId tid = getTopicId(topicId);

            Dictionary<String, Object> prop = new Hashtable<String, Object>();
            prop.put("message.topic", topicId);
            prop.put("message.data", message.getBody());
            prop.put("message.destination", message.getDestination());
            publisher.send(prop);
            log.debug("AMF -> EA: " + topicId);

            reply = new AcknowledgeMessage(message);
            reply.setMessageId(message.getMessageId());
        } else {
            log.warn("Channel %s tried to publish a message to topic %s", fromChannel, topicId);
            reply = new ErrorMessage(message, null);
            ((ErrorMessage) reply).setFaultString("Server.Access.Denied");
        }

        return reply;
    }

    @Override
    public Object manage(Channel fromChannel, CommandMessage message) {
        AsyncMessage reply = null;

        if (message.getOperation() == CommandMessage.SUBSCRIBE_OPERATION) {
            String subscribeTopicId = TopicId.normalize(((String) message.getHeader(AsyncMessage.SUBTOPIC_HEADER)));

            if (getSecurityPolicy().canSubscribe(fromChannel, subscribeTopicId, message)) {
                OSGiTopic topic = getTopic(subscribeTopicId);
                if (topic == null && getSecurityPolicy().canCreate(fromChannel, subscribeTopicId, message))
                    topic = getTopic(subscribeTopicId, true);

                if (topic != null) {
                    topicChannels.put(topic.getId(), fromChannel);
                    String subscriptionId = (String) message.getHeader(AsyncMessage.DESTINATION_CLIENT_ID_HEADER);
                    String selector = (String) message.getHeader(CommandMessage.SELECTOR_HEADER);
                    if (subscriptionId == null)
                        log.warn("No subscriptionId for subscription message");
                    else
                        topic.subscribe(fromChannel, message.getDestination(), subscriptionId, selector, noLocal);

                    reply = new AcknowledgeMessage(message);
                } else {
                    reply = new ErrorMessage(message, null);
                    ((ErrorMessage) reply).setFaultString("cannot create");
                }
            } else {
                reply = new ErrorMessage(message, null);
                ((ErrorMessage) reply).setFaultString("cannot subscribe");
            }
        } else if (message.getOperation() == CommandMessage.UNSUBSCRIBE_OPERATION) {
            String unsubscribeTopicId = TopicId.normalize(((String) message.getHeader(AsyncMessage.SUBTOPIC_HEADER)));

            OSGiTopic topic = getTopic(unsubscribeTopicId);
            String subscriptionId = null;
            if (topic != null) {
                subscriptionId = (String) message.getHeader(AsyncMessage.DESTINATION_CLIENT_ID_HEADER);
                if (subscriptionId == null)
                    log.warn("No subscriptionId for unsubscription message");
                else
                    topic.unsubscribe(fromChannel, subscriptionId);
            }

            reply = new AcknowledgeMessage(message);
            reply.setHeader(AsyncMessage.DESTINATION_CLIENT_ID_HEADER, subscriptionId);
        } else {
            reply = new ErrorMessage(message, null);
            ((ErrorMessage) reply).setFaultString("unknown operation");

        }

        return reply;
    }

    @org.apache.felix.ipojo.handlers.event.Subscriber(
            name = "GravitySubscriber"
    )
    public final void receive(final Event event) {
        try {
            String topicId = (String) event.getProperty("message.topic");
            String destination = (String) event.getProperty("message.destination");
            Object data = event.getProperty("message.data");
            log.debug("EA -> AMF: " + topicId);
            Channel channel = topicChannels.get(topicId);
            if (channel != null) {
                TopicId tid = getTopicId(topicId);
                AsyncMessage message = new AsyncMessage();
                message.setDestination(destination);
                message.setBody(data);
                rootTopic.publish(tid, channel, message);
            }
        } catch (Exception e) {
            log.warn(e, "Error during transmission to topic");
        }
    }

    public String getId() {
        return ID;
    }
}
