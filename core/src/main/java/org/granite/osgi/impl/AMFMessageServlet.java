package org.granite.osgi.impl;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.flex.IChannel;
import org.granite.context.GraniteContext;
import org.granite.context.IGraniteContext;
import org.granite.logging.Logger;
import org.granite.messaging.amf.AMF0Message;
import org.granite.messaging.amf.io.AMF0Deserializer;
import org.granite.messaging.amf.io.AMF0Serializer;
import org.granite.messaging.amf.process.AMF0MessageProcessor;

import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
@Instantiate
public class AMFMessageServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(AMFMessageServlet.class);

    @Requires
    private HttpService httpService;

    @Requires
    private IGraniteContext graniteContext;

    private HttpContext httpContext;

    private Map<String, String> aliases = new HashMap<String, String>();

    @Validate
    private void starting() {
        LOG.debug("GraniteDS's AMFMessageServlet started");

        httpContext = httpService.createDefaultHttpContext();
    }

    @Invalidate
    private void stopping() {
        LOG.debug("GraniteDS's AMFMessageServlet stopped");

        // Remove all aliases
        for (Iterator<String> it = aliases.keySet().iterator(); it.hasNext();) {
            String uri = it.next();
            httpService.unregister(uri);
            it.remove();
            LOG.info("Remove alias: " + uri);
        }
    }

    @Bind(aggregate = true, optional = true)
    private synchronized void bindChannel(final IChannel channel) {
        try {
            if (channel.getClassName().equals(
                    "mx.messaging.channels.AMFChannel")) {

                String uri = aliases.get(channel.getId());

                if (uri == null) {
                    uri = channel.getEndPoint().getUri();
                    httpService.registerServlet(uri, this, null, httpContext);

                    aliases.put(channel.getId(), uri);

                    LOG.info("Add alias: " + uri);
                } else {
                    LOG.warn("Try to add a existing channel: "
                                     + channel.getId());
                }
            } else {
                LOG.debug("Ignored channel : " + channel.getId());
            }

        } catch (Exception e) {
            LOG.error(e, "Can't add channel: " + channel.getId());
        }
    }

    @Unbind
    private synchronized void unbindChannel(final IChannel channel) {
        try {
            if (channel.getClassName().equals(
                    "mx.messaging.channels.AMFChannel")) {

                String uri = aliases.get(channel.getId());

                if (uri != null) {
                    LOG.info("Remove alias: " + uri);
                    aliases.remove(channel.getId());
                    httpService.unregister(uri);
                } else {
                    LOG.warn("Try to remove an unnregistred channel: "
                                     + channel.getId());
                }
            } else {
                LOG.debug("Ignore remove channel: " + channel.getId());
            }
        } catch (Exception e) {
            LOG.error(e, "Can't remove channel: " + channel.getId());
        }
    }

    @Override
    protected final void doPost(final HttpServletRequest request,
                                final HttpServletResponse response) throws
            ServletException, IOException {

        if (graniteContext == null) {
            LOG.error("Could not handle AMF request: GraniteContext " +
                              "uninitialized");
            return;
        }
        try {
            IGraniteContext context = new HttpGraniteContext(graniteContext, request, response);
            GraniteContext.setCurrentInstance(context);

            if (context == null) {
                throw new ServletException("GraniteContext not Initialized!!");
            }

            // Phase1 Deserializing AMF0 request
            AMF0Deserializer deserializer = new AMF0Deserializer(
                    new DataInputStream(request.getInputStream()));
            AMF0Message amf0Request = deserializer.getAMFMessage();

            // Phase2 Processing AMF0 request
            LOG.debug(">>>>> Processing AMF0 request: " + amf0Request);
            AMF0Message amf0Response = AMF0MessageProcessor.process(
                    amf0Request);
            LOG.debug("<<<<< Returning AMF0 response: " + amf0Response);

            // Phase3 Serializing AMF0 response
            response.setContentType(AMF0Message.CONTENT_TYPE);
            AMF0Serializer serializer = new AMF0Serializer(
                    new DataOutputStream(response.getOutputStream()));
            serializer.serializeMessage(amf0Response);

        } catch (Exception e) {
            LOG.error(e, "Could not handle AMF request");
        }
    }
}
