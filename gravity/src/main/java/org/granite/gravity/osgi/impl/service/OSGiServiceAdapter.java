package org.granite.gravity.osgi.impl.service;

import flex.messaging.messages.AcknowledgeMessage;
import flex.messaging.messages.AsyncMessage;
import flex.messaging.messages.CommandMessage;
import flex.messaging.messages.ErrorMessage;

import org.apache.felix.ipojo.annotations.*;
import org.apache.felix.ipojo.handlers.event.publisher.Publisher;

import org.granite.gravity.Channel;
import org.granite.gravity.adapters.ServiceAdapter;
import org.granite.gravity.adapters.TopicId;
import org.granite.logging.Logger;
import org.granite.messaging.service.ServiceException;
import org.granite.osgi.service.GraniteAdapter;
import org.granite.util.XMap;

import org.osgi.service.event.Event;

import java.util.concurrent.ConcurrentHashMap;

@Component (name = "org.granite.gravity.osgi.OSGiServiceAdapter")
@Provides
public class OSGiServiceAdapter extends ServiceAdapter implements GraniteAdapter {

    private static final Logger log = Logger.getLogger(OSGiServiceAdapter.class);

    private final OSGiTopic rootTopic = new OSGiTopic("/", this);
    private transient ConcurrentHashMap<String, TopicId> _topicIdCache = new ConcurrentHashMap<String, TopicId>();

    @Property(name = "ID", mandatory = true)
    private String ID;

    private boolean noLocal = false;

    @org.apache.felix.ipojo.handlers.event.Publisher(
            name = "GravityPublisher",
            topics = "GravityDefaultTopic",
            synchronous = false
    )

    private Publisher publisher;

    @Validate
    public void starting() {
        log.debug("Start OSGiServiceAdapter \""+ ID +"\"");
    }

    @Invalidate
    public void stopping() {
        log.debug("Stop OSGiServiceAdapter \""+ ID +"\"");
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

            //try {
            //fromChannel.publish(new AsyncPublishedMessage(rootTopic, tid, message));
            reply = new AcknowledgeMessage(message);
            reply.setMessageId(message.getMessageId());
            /*}
               catch (MessagePublishingException e) {
                   log.error(e, "Error while publishing message: %s from channel %s to topic: %s", message, fromChannel, tid);
                   reply = new ErrorMessage(message, null);
                   ((ErrorMessage)reply).setFaultString("Server.Publish.Error");
               } */
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

    public String getId() {
        return ID;
    }

    @org.apache.felix.ipojo.handlers.event.Subscriber(
            name = "GravitySubscriber",
            topics = "GravityDefaultTopic"
    )
    public final void receive(final Event e) {

    }
}
