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

package org.granite.osgi.impl;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.context.GraniteContext;
import org.granite.context.GraniteManager;
import org.granite.logging.Logger;
import org.granite.messaging.amf.AMF0Message;
import org.granite.messaging.amf.io.AMF0Deserializer;
import org.granite.messaging.amf.io.AMF0Serializer;

import org.granite.messaging.amf.process.AMF0MessageProcessor;
import org.granite.osgi.HttpGraniteContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class OSGiGraniteMessageServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(OSGiGraniteMessageServlet.class);

    @Requires
    private HttpService httpService;

    @Requires
    private GraniteContext graniteContext;

    @Requires(id = "context")
    private HttpContext httpContext;

    @Property(name = "URI")
    private String uri;

    @Validate
    private void starting() throws ServletException, NamespaceException {
        log.debug("Start OSGiGraniteMessageServlet: " + uri);
        httpService.registerServlet(uri, this, null, httpContext);
    }

    @Invalidate
    private void stopping() {
        log.debug("Stop OSGiGraniteMessageServlet: " + uri);
        httpService.unregister(uri);
    }

    @Override
    protected final void doPost(final HttpServletRequest request, final HttpServletResponse response) throws
            ServletException, IOException {

        if (graniteContext == null) {
            log.error("Could not handle AMF request: GraniteContext uninitialized");
            return;
        }
        try {
            GraniteContext context = new HttpGraniteContext(graniteContext, request, response);
            GraniteManager.setCurrentInstance(context);

            // Phase1 Deserializing AMF0 request
            AMF0Deserializer deserializer = new AMF0Deserializer(new DataInputStream(request.getInputStream()));
            AMF0Message amf0Request = deserializer.getAMFMessage();

            // Phase2 Processing AMF0 request
            log.debug(">>>>> Processing AMF0 request: " + amf0Request);
            AMF0Message amf0Response = AMF0MessageProcessor.process(amf0Request);
            log.debug("<<<<< Returning AMF0 response: " + amf0Response);

            // Phase3 Serializing AMF0 response
            response.setContentType(AMF0Message.CONTENT_TYPE);
            AMF0Serializer serializer = new AMF0Serializer(new DataOutputStream(response.getOutputStream()));
            serializer.serializeMessage(amf0Response);

        } catch (Exception e) {
            log.error(e, "Could not handle AMF request");
        }
    }
}
