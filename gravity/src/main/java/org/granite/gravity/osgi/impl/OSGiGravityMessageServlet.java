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
import flex.messaging.messages.Message;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.flex.Channel;
import org.granite.context.AMFContextImpl;
import org.granite.context.GraniteContext;
import org.granite.context.GraniteManager;
import org.granite.gravity.AbstractGravityServlet;
import org.granite.gravity.AsyncHttpContext;
import org.granite.gravity.Gravity;
import org.granite.gravity.GravityManager;
import org.granite.gravity.generic.GenericChannel;
import org.granite.gravity.generic.WaitingContinuation;
import org.granite.logging.Logger;
import org.granite.osgi.HttpGraniteContext;

import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class OSGiGravityMessageServlet extends AbstractGravityServlet {
    private static final Logger log = Logger.getLogger(OSGiGravityMessageServlet.class);

    @Requires
    private HttpService httpService;

    @Requires
    private Gravity gravity;

    @Requires(id = "context")
    private HttpContext httpContext;

    @Property(name = "URI")
    private String uri;

    @Validate
    private void starting() throws ServletException, NamespaceException {
        log.debug("Start OSGiGravityMessageServlet: " + uri);
        httpService.registerServlet(uri, this, null, httpContext);
    }

    @Invalidate
    private void stopping() {
        log.debug("Stop OSGiGravityMessageServlet: " + uri);
        httpService.unregister(uri);
    }


    @Override
    protected final void doPost(final HttpServletRequest request, final HttpServletResponse response) throws
            ServletException, IOException {

        log.debug("doPost: from %s:%d", request.getRemoteAddr(), request.getRemotePort());


        if (gravity == null) {
            log.error("Could not handle AMF request: Gravity uninitialized");
            return;
        }
        try {
            /* Set Gravity context */
            GravityManager.setGravity(gravity, getServletContext());
            gravity.getGravityConfig().getChannelFactory().init(gravity.getGravityConfig(), getServletConfig());
            GraniteContext context = new HttpGraniteContext(gravity.initThread(), request, response);
            GraniteManager.setCurrentInstance(context);

            AsyncMessage connect = getConnectMessage(request);

            // Resumed request (new received messages or timeout).
            if (connect != null) {
                try {
                    String channelId = (String) connect.getClientId();
                    GenericChannel channel = (GenericChannel) gravity.getChannel(channelId);
                    // Reset channel continuation instance and deliver pending messages.
                    synchronized (channel) {
                        channel.reset();
                        channel.runReceived(new AsyncHttpContext(request, response, connect));
                    }
                } finally {
                    removeConnectMessage(request);
                }
                return;
            }

            // New Request.
            Message[] amf3Requests = deserialize(gravity, request);
            log.debug(">> [AMF3 REQUESTS] %s", (Object) amf3Requests);

            Message[] amf3Responses = null;

            boolean accessed = false;
            for (int i = 0; i < amf3Requests.length; i++) {
                Message amf3Request = amf3Requests[i];
                ((AMFContextImpl) context.getAMFContext()).setCurrentAmf3Message(amf3Request);

                // Ask gravity to create a specific response (will be null for connect request from tunnel).
                Message amf3Response = gravity.handleMessage(amf3Request);
                String channelId = (String) amf3Request.getClientId();

                // Mark current channel (if any) as accessed.
                if (!accessed)
                    accessed = gravity.access(channelId);

                // (Re)Connect message from tunnel.
                if (amf3Response == null) {
                    if (amf3Requests.length > 1)
                        throw new IllegalArgumentException("Only one request is allowed on tunnel.");

                    GenericChannel channel = (GenericChannel) gravity.getChannel(channelId);
                    if (channel == null)
                        throw new NullPointerException("No channel on tunnel connect");

                    // Try to send pending messages if any (using current container thread).
                    if (!channel.runReceived(new AsyncHttpContext(request, response, amf3Request))) {
                        // No pending messages, wait for new ones or timeout.
                        setConnectMessage(request, amf3Request);
                        synchronized (channel) {
                            WaitingContinuation continuation = new WaitingContinuation(channel);
                            channel.setContinuation(continuation);
                            continuation.suspend(gravity.getGravityConfig().getLongPollingTimeoutMillis());
                        }
                    }

                    return;
                }

                if (amf3Responses == null)
                    amf3Responses = new Message[amf3Requests.length];
                amf3Responses[i] = amf3Response;
            }

            log.debug("<< [AMF3 RESPONSES] %s", (Object) amf3Responses);

            serialize(gravity, response, amf3Responses);
        } catch (IOException e) {
            log.error(e, "Gravity message error");
            throw e;
        } catch (ClassNotFoundException e) {
            log.error(e, "Gravity message error");
            throw new ServletException("Gravity message error", e);
        } finally {
            // Cleanup context (thread local GraniteContext, etc.)
            cleanupRequest(request);
        }

        removeConnectMessage(request);
    }
}
