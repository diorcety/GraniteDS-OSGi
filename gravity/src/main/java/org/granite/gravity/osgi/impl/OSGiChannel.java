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

package org.granite.gravity.osgi.impl;

import flex.messaging.messages.AsyncMessage;

import org.granite.context.AMFContextImpl;
import org.granite.context.GraniteContext;
import org.granite.context.GraniteManager;
import org.granite.gravity.AsyncHttpContext;
import org.granite.gravity.Gravity;
import org.granite.gravity.GravityConfig;
import org.granite.gravity.generic.GenericChannel;
import org.granite.logging.Logger;
import org.granite.messaging.amf.AMF0Message;
import org.granite.osgi.HttpGraniteContext;

import java.io.IOException;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.util.LinkedList;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class OSGiChannel extends GenericChannel {
    private static final Logger log = Logger.getLogger(OSGiChannel.class);

    private GraniteContext granitecontext;
    public OSGiChannel(GraniteContext granitecontext, ServletConfig servletConfig, GravityConfig gravityConfig, String id) {
        super(servletConfig, gravityConfig, id);
        this.granitecontext = granitecontext;
    }

    @Override
    public boolean runReceived(AsyncHttpContext asyncHttpContext) {

        boolean httpAsParam = (asyncHttpContext != null);
        LinkedList<AsyncMessage> messages = null;
        OutputStream os = null;

        try {
            receivedQueueLock.lock();
            try {
                // Do we have any pending messages?
                if (receivedQueue.isEmpty())
                    return false;

                // Do we have a valid http context?
                if (asyncHttpContext == null) {
                    asyncHttpContext = acquireAsyncHttpContext();
                    if (asyncHttpContext == null)
                        return false;
                }

                // Both conditions are ok, get all pending messages.
                messages = receivedQueue;
                receivedQueue = new LinkedList<AsyncMessage>();
            } finally {
                receivedQueueLock.unlock();
            }

            HttpServletRequest request = asyncHttpContext.getRequest();
            HttpServletResponse response = asyncHttpContext.getResponse();

            // Set response messages correlation ids to connect request message id.
            String correlationId = asyncHttpContext.getConnectMessage().getMessageId();
            AsyncMessage[] messagesArray = new AsyncMessage[messages.size()];
            int i = 0;
            for (AsyncMessage message : messages) {
                message.setCorrelationId(correlationId);
                messagesArray[i++] = message;
            }

            // Setup serialization context (thread local)
            Gravity gravity = getGravity();
            GraniteContext context = new HttpGraniteContext(granitecontext, request, response);
            GraniteManager.setCurrentInstance(context);
            ((AMFContextImpl) context.getAMFContext()).setCurrentAmf3Message(asyncHttpContext.getConnectMessage());

            // Write messages to response output stream.

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(AMF0Message.CONTENT_TYPE);
            response.setDateHeader("Expire", 0L);
            response.setHeader("Cache-Control", "no-store");

            os = response.getOutputStream();
            ObjectOutput amf3Serializer = context.getGraniteConfig().newAMF3Serializer(os);

            log.debug("<< [MESSAGES for channel=%s] %s", this, messagesArray);

            amf3Serializer.writeObject(messagesArray);

            os.flush();
            response.flushBuffer();

            return true; // Messages were delivered, http context isn't valid anymore.
        } catch (IOException e) {
            log.warn(e, "Could not send messages to channel: %s (retrying later)", this);

            GravityConfig gravityConfig = getGravity().getGravityConfig();
            if (messages != null && gravityConfig.isRetryOnError()) {
                receivedQueueLock.lock();
                try {
                    if (receivedQueue.size() + messages.size() > gravityConfig.getMaxMessagesQueuedPerChannel()) {
                        log.warn(
                                "Channel %s has reached its maximum queue capacity %s (throwing %s messages)",
                                this,
                                gravityConfig.getMaxMessagesQueuedPerChannel(),
                                messages.size()
                        );
                    } else
                        receivedQueue.addAll(0, messages);
                } finally {
                    receivedQueueLock.unlock();
                }
            }

            return true; // Messages weren't delivered, but http context isn't valid anymore.
        } finally {

            // Cleanup serialization context (thread local)
            try {
                GraniteManager.release();
            } catch (Exception e) {
                // should never happen...
            }

            // Close output stream.
            try {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        log.warn(e, "Could not close output stream (ignored)");
                    }
                }
            } finally {
                // Cleanup http context (only if this method wasn't explicitly called with a non null
                // AsyncHttpContext from the servlet).
                if (!httpAsParam)
                    releaseAsyncHttpContext(asyncHttpContext);
            }
        }
    }
}
