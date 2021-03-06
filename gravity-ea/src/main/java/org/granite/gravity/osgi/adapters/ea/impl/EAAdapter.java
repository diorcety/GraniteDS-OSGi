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

import flex.messaging.messages.AcknowledgeMessage;
import flex.messaging.messages.AsyncMessage;
import flex.messaging.messages.CommandMessage;
import flex.messaging.messages.ErrorMessage;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;

import org.apache.felix.ipojo.annotations.*;

import org.granite.gravity.Channel;
import org.granite.gravity.osgi.adapters.ea.EAConstants;
import org.granite.logging.Logger;
import org.granite.osgi.GraniteConstants;
import org.granite.osgi.service.GraniteAdapter;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;

@Component
@Instantiate
@Provides(properties = @StaticServiceProperty(name = "ID", value = EAConstants.ADAPTER_ID, type = "string"))
public class EAAdapter implements GraniteAdapter {
    private static final Logger log = Logger.getLogger(EAAdapter.class);

    @Requires(from = GraniteConstants.ADAPTER)
    private Factory adapterFactory;

    private Collection<EAClient> clients = new LinkedList<EAClient>();

    private ComponentInstance configuration;

    private EAAdapter() {

    }

    @Validate
    private void start() throws MissingHandlerException, ConfigurationException, UnacceptableConfiguration {
        log.debug("Start EAAdapter");

        {
            Dictionary properties = new Hashtable();
            properties.put("id", getId());
            configuration = adapterFactory.createComponentInstance(properties);
        }
    }

    @Invalidate
    private void stop() {
        log.debug("Stop EAAdapter");
        configuration.dispose();
    }

    @Bind(aggregate = true, optional = true)
    private void bindClient(EAClient client) {
        synchronized (clients) {
            clients.add(client);
        }
    }

    @Unbind
    private void unbindClient(EAClient client) {
        synchronized (clients) {
            clients.remove(client);
        }
    }

    private EAClient getClient(String destination) {
        synchronized (clients) {
            for (EAClient client : clients) {
                if (client.getDestination().equals(destination))
                    return client;
            }
        }

        throw new RuntimeException("Can't found the EventAdmin destination: " + destination);
    }

    @Override
    public Object invoke(Channel fromClient, AsyncMessage message) {
        try {
            EAClient client = getClient(message.getDestination());
            client.send(message);

            AsyncMessage reply = new AcknowledgeMessage(message);
            reply.setMessageId(message.getMessageId());

            return reply;
        } catch (Exception e) {
            log.error(e, "Error sending message");
            ErrorMessage error = new ErrorMessage(message, null);
            error.setFaultString("EA Adapter error " + e.getMessage());

            return error;
        }
    }

    @Override
    public String getId() {
        return EAConstants.ADAPTER_ID;
    }

    @Override
    public Object manage(Channel fromChannel, CommandMessage message) {
        if (message.getOperation() == CommandMessage.SUBSCRIBE_OPERATION) {
            try {
                EAClient client = getClient(message.getDestination());
                client.subscribe(fromChannel, message);

                AsyncMessage reply = new AcknowledgeMessage(message);
                return reply;
            } catch (Exception e) {
                throw new RuntimeException("EAAdapter subscribe error on topic: " + message, e);
            }
        } else if (message.getOperation() == CommandMessage.UNSUBSCRIBE_OPERATION) {
            try {
                EAClient client = getClient(message.getDestination());
                client.unsubscribe(fromChannel, message);

                AsyncMessage reply = new AcknowledgeMessage(message);
                return reply;
            } catch (Exception e) {
                throw new RuntimeException("EAAdapter unsubscribe error on topic: " + message, e);
            }
        }

        return null;
    }
}
