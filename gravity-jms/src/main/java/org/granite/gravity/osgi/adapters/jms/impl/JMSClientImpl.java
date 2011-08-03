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

package org.granite.gravity.osgi.adapters.jms.impl;

import flex.messaging.messages.AsyncMessage;
import flex.messaging.messages.CommandMessage;
import flex.messaging.messages.Message;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.gravity.Channel;
import org.granite.gravity.MessageReceivingException;
import org.granite.gravity.osgi.adapters.jms.JMSConstants;
import org.granite.logging.Logger;
import org.granite.messaging.service.ServiceException;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Component(name = JMSConstants.CONFIGURATION_ID)
@Provides
public class JMSClientImpl implements JMSClient {

    private static final Logger log = Logger.getLogger(JMSClientImpl.class);

    @Property(name = "destination", mandatory = true)
    private String destination;

    private boolean transactedSessions = false;

    @Property(name = "transacted-sessions", mandatory = false)
    private void setTransactedSessions(String mode) {
        if (Boolean.TRUE.toString().equals(mode))
            transactedSessions = true;
    }


    private int acknowledgeMode = Session.AUTO_ACKNOWLEDGE;

    @Property(name = "acknowledge-mode", mandatory = false)
    private void setAcknowledgeMode(String mode) {
        if ("AUTO_ACKNOWLEDGE".equals(mode))
            acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
        else if ("CLIENT_ACKNOWLEDGE".equals(mode))
            acknowledgeMode = Session.CLIENT_ACKNOWLEDGE;
        else if ("DUPS_OK_ACKNOWLEDGE".equals(mode))
            acknowledgeMode = Session.DUPS_OK_ACKNOWLEDGE;
    }


    private boolean textMessages = false;

    @Property(name = "message-type", mandatory = false)
    private void setMessageType(String message) {
        if ("javax.jms.TextMessage".equals(message))
            textMessages = true;
    }


    private boolean noLocal = false;

    @Property(name = "no-local", mandatory = false)
    private void setNoLocal(String mode) {
        if (Boolean.TRUE.toString().equals(mode))
            noLocal = true;
    }


    @Property(name = "initial-context-environment", mandatory = false)
    Map<String, String> initialContextEnvironment;

    @Property(name = "connection-factory", mandatory = true)
    private String cfJndiName;

    @Property(name = "destination-jndi-name", mandatory = true)
    private String dsJndiName;


    protected int messagePriority = javax.jms.Message.DEFAULT_PRIORITY;
    protected int deliveryMode = javax.jms.Message.DEFAULT_DELIVERY_MODE;
    protected ConnectionFactory jmsConnectionFactory = null;
    protected javax.jms.Destination jmsDestination = null;

    private javax.jms.Connection jmsConnection = null;
    private javax.jms.Session jmsProducerSession = null;
    private javax.jms.MessageProducer jmsProducer = null;
    private Map<String, JMSConsumer> consumers = new HashMap<String, JMSConsumer>();

    private JMSClientImpl() {
    }

    @Validate
    private void start() throws NamingException, ServiceException {
        log.debug("Start JMSClient: " + toString());

        // Environment
        Properties environment = new Properties();
        if (initialContextEnvironment != null)
            environment.putAll(initialContextEnvironment);

        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        InitialContext ic = new InitialContext(environment);
        jmsConnectionFactory = (ConnectionFactory) ic.lookup(cfJndiName);
        jmsDestination = (Destination) ic.lookup(dsJndiName);

        try {
            jmsConnection = jmsConnectionFactory.createConnection();
            jmsConnection.start();
        } catch (JMSException e) {
            throw new ServiceException("JMS Initialize error", e);
        }
    }

    @Invalidate
    private void stop() throws ServiceException {
        log.debug("Stop JMSClient: " + toString());

        try {
            if (jmsProducer != null)
                jmsProducer.close();
            if (jmsProducerSession != null)
                jmsProducerSession.close();
            for (JMSConsumer consumer : consumers.values())
                consumer.close();
            jmsConnection.stop();
            jmsConnection.close();
        } catch (JMSException e) {
            throw new ServiceException("JMS Stop error", e);
        }
    }

    public String getDestination() {
        return destination;
    }

    public void send(Message message) throws Exception {
        if (jmsProducerSession == null)
            jmsProducerSession = jmsConnection.createSession(transactedSessions, acknowledgeMode);
        if (jmsProducer == null) {
            jmsProducer = jmsProducerSession.createProducer(jmsDestination);
            jmsProducer.setPriority(messagePriority);
            jmsProducer.setDeliveryMode(deliveryMode);
        }

        Object msg = message.getBody();

        javax.jms.Message jmsMessage = null;
        if (textMessages)
            jmsMessage = jmsProducerSession.createTextMessage(msg.toString());
        else
            jmsMessage = jmsProducerSession.createObjectMessage((Serializable) msg);

        jmsMessage.setJMSMessageID(normalizeJMSMessageID(message.getMessageId()));
        jmsMessage.setJMSCorrelationID(normalizeJMSMessageID(((AsyncMessage) message).getCorrelationId()));
        jmsMessage.setJMSTimestamp(message.getTimestamp());
        jmsMessage.setJMSExpiration(message.getTimeToLive());

        for (Map.Entry<String, Object> me : message.getHeaders().entrySet()) {
            if ("JMSType".equals(me.getKey())) {
                if (me.getValue() instanceof String)
                    jmsMessage.setJMSType((String) me.getValue());
            } else if ("JMSPriority".equals(me.getKey())) {
                if (me.getValue() instanceof Integer)
                    jmsMessage.setJMSPriority(((Integer) me.getValue()).intValue());
            } else if (me.getValue() instanceof String)
                jmsMessage.setStringProperty(me.getKey(), (String) me.getValue());
            else if (me.getValue() instanceof Boolean)
                jmsMessage.setBooleanProperty(me.getKey(), ((Boolean) me.getValue()).booleanValue());
            else if (me.getValue() instanceof Integer)
                jmsMessage.setIntProperty(me.getKey(), ((Integer) me.getValue()).intValue());
            else if (me.getValue() instanceof Long)
                jmsMessage.setLongProperty(me.getKey(), ((Long) me.getValue()).longValue());
            else if (me.getValue() instanceof Double)
                jmsMessage.setDoubleProperty(me.getKey(), ((Double) me.getValue()).doubleValue());
            else
                jmsMessage.setObjectProperty(me.getKey(), me.getValue());
        }

        jmsProducer.send(jmsMessage);
        if (transactedSessions)
            jmsProducerSession.commit();
    }

    private String normalizeJMSMessageID(String messageId) {
        if (messageId != null && !messageId.startsWith("ID:"))
            messageId = "ID:" + messageId;
        return messageId;
    }

    public void subscribe(Channel channel, Message message) throws Exception {
        String subscriptionId = (String) message.getHeader(AsyncMessage.DESTINATION_CLIENT_ID_HEADER);
        String selector = (String) message.getHeader(CommandMessage.SELECTOR_HEADER);
        String topic = (String) message.getHeader(AsyncMessage.SUBTOPIC_HEADER);

        synchronized (consumers) {
            JMSConsumer consumer = consumers.get(subscriptionId);
            if (consumer == null) {
                consumer = new JMSConsumer(channel, subscriptionId, selector, noLocal);
                consumers.put(subscriptionId, consumer);
            } else
                consumer.setSelector(selector);
            channel.addSubscription(message.getDestination(), topic, subscriptionId, false);
        }
    }

    public void unsubscribe(Channel channel, Message message) throws Exception {
        String subscriptionId = (String) message.getHeader(AsyncMessage.DESTINATION_CLIENT_ID_HEADER);

        synchronized (consumers) {
            JMSConsumer consumer = consumers.get(subscriptionId);
            if (consumer != null)
                consumer.close();
            consumers.remove(subscriptionId);
            channel.removeSubscription(subscriptionId);
        }
    }


    private class JMSConsumer implements MessageListener {

        private Channel channel = null;
        private String subscriptionId = null;
        private javax.jms.Session jmsConsumerSession = null;
        private javax.jms.MessageConsumer jmsConsumer = null;
        private boolean noLocal = false;

        public JMSConsumer(Channel channel, String subscriptionId, String selector, boolean noLocal) throws JMSException {
            this.channel = channel;
            this.subscriptionId = subscriptionId;
            this.noLocal = noLocal;
            jmsConsumerSession = jmsConnection.createSession(transactedSessions, acknowledgeMode);
            jmsConsumer = jmsConsumerSession.createConsumer(jmsDestination, selector, noLocal);
            jmsConsumer.setMessageListener(this);
        }

        public void setSelector(String selector) throws JMSException {
            if (jmsConsumer != null)
                jmsConsumer.close();
            jmsConsumer = jmsConsumerSession.createConsumer(jmsDestination, selector, noLocal);
            jmsConsumer.setMessageListener(this);
        }

        public void close() throws JMSException {
            if (jmsConsumer != null)
                jmsConsumer.close();
            if (jmsConsumerSession != null)
                jmsConsumerSession.close();
        }

        public void onMessage(javax.jms.Message message) {
            if (!(message instanceof ObjectMessage) && !(message instanceof TextMessage)) {
                log.error("JMS Adapter message type not allowed: %s", message.getClass().getName());

                try {
                    if (acknowledgeMode == Session.CLIENT_ACKNOWLEDGE)
                        message.acknowledge();

                    if (transactedSessions)
                        jmsConsumerSession.commit();
                } catch (JMSException e) {
                    log.error(e, "Could not ack/commit JMS onMessage");
                }
            }

            log.debug("Delivering JMS message");

            AsyncMessage dmsg = new AsyncMessage();
            try {
                Serializable msg = null;

                if (textMessages) {
                    TextMessage jmsMessage = (TextMessage) message;
                    msg = jmsMessage.getText();
                } else {
                    ObjectMessage jmsMessage = (ObjectMessage) message;
                    msg = jmsMessage.getObject();
                }

                dmsg.setDestination(destination);
                dmsg.setBody(msg);
                dmsg.setMessageId(denormalizeJMSMessageID(message.getJMSMessageID()));
                dmsg.setCorrelationId(denormalizeJMSMessageID(message.getJMSCorrelationID()));
                dmsg.setTimestamp(message.getJMSTimestamp());
                dmsg.setTimeToLive(message.getJMSExpiration());

                Enumeration<?> ename = message.getPropertyNames();
                while (ename.hasMoreElements()) {
                    String pname = (String) ename.nextElement();
                    dmsg.setHeader(pname, message.getObjectProperty(pname));
                }

                dmsg.setHeader("JMSType", message.getJMSType());
                dmsg.setHeader("JMSPriority", Integer.valueOf(message.getJMSPriority()));
                dmsg.setHeader("JMSRedelivered", Boolean.valueOf(message.getJMSRedelivered()));
                dmsg.setHeader("JMSDeliveryMode", Integer.valueOf(message.getJMSDeliveryMode()));
                dmsg.setHeader(AsyncMessage.DESTINATION_CLIENT_ID_HEADER, subscriptionId);

                channel.receive(dmsg);
            } catch (JMSException e) {
                if (transactedSessions) {
                    try {
                        jmsConsumerSession.rollback();
                    } catch (JMSException f) {
                        log.error("Could not rollback JMS session, messageId: %s", dmsg.getMessageId());
                    }
                }

                throw new RuntimeException("JMS Error", e);
            } catch (MessageReceivingException e) {
                if (transactedSessions) {
                    try {
                        jmsConsumerSession.rollback();
                    } catch (JMSException f) {
                        log.error("Could not rollback JMS session, messageId: %s", dmsg.getMessageId());
                    }
                }

                throw new RuntimeException("Channel delivery Error", e);
            }

            try {
                if (acknowledgeMode == Session.CLIENT_ACKNOWLEDGE)
                    message.acknowledge();

                if (transactedSessions)
                    jmsConsumerSession.commit();
            } catch (JMSException e) {
                log.error("Could not ack/commit JMS onMessage, messageId: %s", dmsg.getMessageId());

                // Message already delivered, should rollback or not ?
            }
        }

        private String denormalizeJMSMessageID(String messageId) {
            if (messageId != null && messageId.startsWith("ID:"))
                messageId = messageId.substring(3);
            return messageId;
        }
    }


    @Override
    public String toString() {
        return "JMSConfiguration{" +
                "destination=" + destination +
                ", transacted-sessions=" + transactedSessions +
                ", acknowledge-mode='" + acknowledgeMode + '\'' +
                ", message-type=" + textMessages +
                ", no-local=" + noLocal +
                ", initial-context-environment=" + initialContextEnvironment +
                ", connection-factory='" + cfJndiName + '\'' +
                ", destination-jndi-name='" + dsJndiName + '\'' +
                '}';
    }
}
