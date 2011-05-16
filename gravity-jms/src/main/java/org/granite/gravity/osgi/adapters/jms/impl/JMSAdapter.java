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

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.annotations.*;

import org.granite.gravity.Channel;
import org.granite.gravity.osgi.adapters.jms.JMSClient;
import org.granite.gravity.osgi.adapters.jms.JMSConstants;
import org.granite.logging.Logger;
import org.granite.osgi.service.GraniteAdapter;

import flex.messaging.messages.AcknowledgeMessage;
import flex.messaging.messages.AsyncMessage;
import flex.messaging.messages.CommandMessage;
import flex.messaging.messages.ErrorMessage;

@Component
@Instantiate
@Provides(properties = @StaticServiceProperty(name = "ID", value = JMSConstants.ADAPTER_ID, type = "string"))
public class JMSAdapter implements GraniteAdapter {

    private static final Logger log = Logger.getLogger(JMSAdapter.class);

    @Requires(from = "org.granite.config.flex.Adapter")
    private Factory adapterFactory;

    @Requires(specification = "org.granite.gravity.osgi.adapters.jms.JMSClient", optional = true)
    private Collection<JMSClient> clients;

    private ComponentInstance configuration;

    private JMSAdapter() {

    }

    @Validate
    private void start() throws MissingHandlerException, ConfigurationException, UnacceptableConfiguration {
        log.debug("Start JMSAdapter");

        {
            Dictionary properties = new Hashtable();
            properties.put("ID", getId());
            configuration = adapterFactory.createComponentInstance(properties);
        }
    }

    @Invalidate
    private void stop() {
        log.debug("Stop JMSAdapter");
        configuration.stop();
    }

    private JMSClient getJMSClient(String destination) {
        for (JMSClient client : clients) {
            if (client.getDestination().equals(destination))
                return client;
        }

        return null;
    }

    @Override
    public Object invoke(Channel fromClient, AsyncMessage message) {
        try {
            JMSClient jmsClient = getJMSClient(message.getDestination());
            jmsClient.send(message);

            AsyncMessage reply = new AcknowledgeMessage(message);
            reply.setMessageId(message.getMessageId());

            return reply;
        } catch (Exception e) {
            log.error(e, "Error sending message");
            ErrorMessage error = new ErrorMessage(message, null);
            error.setFaultString("JMS Adapter error " + e.getMessage());

            return error;
        }
    }

    @Override
    public String getId() {
        return JMSConstants.ADAPTER_ID;
    }

    @Override
    public Object manage(Channel fromChannel, CommandMessage message) {
        if (message.getOperation() == CommandMessage.SUBSCRIBE_OPERATION) {
            try {
                JMSClient jmsClient = getJMSClient(message.getDestination());
                jmsClient.subscribe(fromChannel, message);

                AsyncMessage reply = new AcknowledgeMessage(message);
                return reply;
            } catch (Exception e) {
                throw new RuntimeException("JMSAdapter subscribe error on topic: " + message, e);
            }
        } else if (message.getOperation() == CommandMessage.UNSUBSCRIBE_OPERATION) {
            try {
                JMSClient jmsClient = getJMSClient(message.getDestination());
                jmsClient.unsubscribe(fromChannel, message);

                AsyncMessage reply = new AcknowledgeMessage(message);
                return reply;
            } catch (Exception e) {
                throw new RuntimeException("JMSAdapter unsubscribe error on topic: " + message, e);
            }
        }

        return null;
    }
}
