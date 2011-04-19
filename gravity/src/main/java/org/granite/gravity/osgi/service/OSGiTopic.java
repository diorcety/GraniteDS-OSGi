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

import flex.messaging.messages.AsyncMessage;
import org.granite.gravity.Channel;
import org.granite.gravity.Subscription;
import org.granite.gravity.adapters.TopicId;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Adapted from Greg Wilkins code (Jetty).
 * 
 * @author William DRAI
 */
public class OSGiTopic {

    private final TopicId id;
    private final OSGiEventAdminAdapter serviceAdapter;

    private ConcurrentMap<String, Subscription> subscriptions = new ConcurrentHashMap<String, Subscription>();
    private ConcurrentMap<String, OSGiTopic> children = new ConcurrentHashMap<String, OSGiTopic>();
    private OSGiTopic wild;
    private OSGiTopic wildWild;


    public OSGiTopic(String topicId, OSGiEventAdminAdapter serviceAdapter) {
        this.id = new TopicId(topicId);
        this.serviceAdapter = serviceAdapter;
    }

    public String getId() {
        return id.toString();
    }

    public TopicId getTopicId() {
        return id;
    }

    public OSGiTopic getChild(TopicId topicId) {
        String next = topicId.getSegment(id.depth());
        if (next == null)
            return null;

        OSGiTopic topic = children.get(next);

        if (topic == null || topic.getTopicId().depth() == topicId.depth()) {
            return topic;
        }
        return topic.getChild(topicId);
    }

    public void addChild(OSGiTopic topic) {
        TopicId child = topic.getTopicId();
        if (!id.isParentOf(child))
            throw new IllegalArgumentException(id + " not parent of " + child);

        String next = child.getSegment(id.depth());

        if ((child.depth() - id.depth()) == 1) {
            // add the topic to this topics
            OSGiTopic old = children.putIfAbsent(next, topic);

            if (old != null)
                throw new IllegalArgumentException("Already Exists");

            if (TopicId.WILD.equals(next))
                wild = topic;
            else if (TopicId.WILDWILD.equals(next))
                wildWild = topic;
        }
        else {
            OSGiTopic branch = serviceAdapter.getTopic((id.depth() == 0 ? "/" : (id.toString() + "/")) + next, true);
            branch.addChild(topic);
        }
    }

    public void subscribe(Channel channel, String destination, String subscriptionId, String selector, boolean noLocal) {
        synchronized (this) {
            Subscription subscription = channel.addSubscription(destination, getId(), subscriptionId, noLocal);
            subscription.setSelector(selector);
            subscriptions.putIfAbsent(subscriptionId, subscription);
        }
    }

    public void unsubscribe(Channel channel, String subscriptionId) {
        synchronized(this) {
        	subscriptions.remove(subscriptionId);
            channel.removeSubscription(subscriptionId);
        }
    }


    public void publish(TopicId to, Channel fromChannel, AsyncMessage msg) {
        int tail = to.depth()-id.depth();

        switch(tail) {
            case 0:
                for (Subscription subscription : subscriptions.values()) {
                    AsyncMessage m = msg.clone();
                    subscription.deliver(fromChannel, m);
                }

                break;

            case 1:
                if (wild != null) {
                    for (Subscription subscription : wild.subscriptions.values()) {
                        AsyncMessage m = msg.clone();
                        subscription.deliver(fromChannel, m);
                    }
                }

            default: {
                if (wildWild != null) {
                    for (Subscription subscription : wildWild.subscriptions.values()) {
                        AsyncMessage m = msg.clone();
                        subscription.deliver(fromChannel, m);
                    }
                }
                String next = to.getSegment(id.depth());
                OSGiTopic topic = children.get(next);
                if (topic != null)
                    topic.publish(to, fromChannel, msg);
            }
        }
    }

    @Override
    public String toString() {
        return id.toString() + " {" + children.values() + "}";
    }
}
