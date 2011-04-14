package org.granite.osgi.impl;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.context.GraniteContext;
import org.granite.logging.Logger;
import org.granite.messaging.amf.AMF0Message;
import org.granite.messaging.amf.process.AMF0MessageProcessor;

import org.granite.osgi.GraniteClassRegistry;
import org.granite.osgi.impl.config.IChannel;
import org.granite.osgi.impl.io.OSGiAMF0Deserializer;
import org.granite.osgi.impl.io.OSGiAMF0Serializer;
import org.granite.osgi.impl.io.OSGiResolver;
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
    private static final Logger log = Logger.getLogger(AMFMessageServlet.class);

    @Requires
    private HttpService httpService;

    @Requires
    private IGraniteContext graniteContext;

    @Requires
    private GraniteClassRegistry classRegistry;

    private HttpContext httpContext;

    private Map<String, String> aliases = new HashMap<String, String>();

    @Validate
    private void starting() {
        log.debug("GraniteDS's AMFMessageServlet started");

        httpContext = httpService.createDefaultHttpContext();
    }

    @Invalidate
    private void stopping() {
        log.debug("GraniteDS's AMFMessageServlet stopped");

        // Remove all aliases
        for (Iterator<String> it = aliases.keySet().iterator(); it.hasNext();) {
            String uri = it.next();
            httpService.unregister(uri);
            it.remove();
            log.info("Remove alias: " + uri);
        }
    }

    @Bind(aggregate = true, optional = true)
    private synchronized void bindChannel(final IChannel channel) {
        try {
            if (channel.getClassName().equals("mx.messaging.channels.AMFChannel")) {

                String uri = aliases.get(channel.getId());

                if (uri == null) {
                    uri = channel.getEndPoint().getUri();
                    httpService.registerServlet(uri, this, null, httpContext);

                    aliases.put(channel.getId(), uri);

                    log.info("Add alias: " + uri);
                } else {
                    log.warn("Try to add a existing channel: " + channel.getId());
                }
            } else {
                log.debug("Ignored channel : " + channel.getId());
            }

        } catch (Exception e) {
            log.error(e, "Can't add channel: " + channel.getId());
        }
    }

    @Unbind
    private synchronized void unbindChannel(final IChannel channel) {
        try {
            if (channel.getClassName().equals("mx.messaging.channels.AMFChannel")) {

                String uri = aliases.get(channel.getId());

                if (uri != null) {
                    log.info("Remove alias: " + uri);
                    aliases.remove(channel.getId());
                    httpService.unregister(uri);
                } else {
                    log.warn("Try to remove an unnregistred channel: " + channel.getId());
                }
            } else {
                log.debug("Ignore remove channel: " + channel.getId());
            }
        } catch (Exception e) {
            log.error(e, "Can't remove channel: " + channel.getId());
        }
    }

    @Override
    protected final void doPost(final HttpServletRequest request,
                                final HttpServletResponse response) throws
            ServletException, IOException {

        if (graniteContext == null) {
            log.error("Could not handle AMF request: GraniteContext uninitialized");
            return;
        }
        try {
            GraniteContext context = new HttpGraniteContext(graniteContext.getGraniteContext(), classRegistry, request, response);
            if (context == null) {
                throw new ServletException("GraniteContext not Initialized!!");
            }
            GraniteContext.setCurrentInstance(context);
            OSGiResolver resolver = new OSGiResolver(context);

            // Phase1 Deserializing AMF0 request

            OSGiAMF0Deserializer deserializer = new OSGiAMF0Deserializer(new DataInputStream(request.getInputStream()));
            AMF0Message amf0Request = deserializer.getAMFMessage();
            amf0Request = (AMF0Message) resolver.resolve(amf0Request);

            // Phase2 Processing AMF0 request
            log.debug(">>>>> Processing AMF0 request: " + amf0Request);
            AMF0Message amf0Response = AMF0MessageProcessor.process(amf0Request);
            log.debug("<<<<< Returning AMF0 response: " + amf0Response);

            // Phase3 Serializing AMF0 response
            response.setContentType(AMF0Message.CONTENT_TYPE);
            OSGiAMF0Serializer serializer = new OSGiAMF0Serializer(new DataOutputStream(response.getOutputStream()));
            serializer.serializeMessage(amf0Response);

        } catch (Exception e) {
            log.error(e, "Could not handle AMF request");
        }
    }
}
