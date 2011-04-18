package org.granite.gravity.osgi.impl;

import flex.messaging.messages.AsyncMessage;
import flex.messaging.messages.Message;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.flex.Channel;
import org.granite.gravity.AbstractGravityServlet;
import org.granite.gravity.AsyncHttpContext;
import org.granite.gravity.generic.GenericChannel;
import org.granite.gravity.generic.WaitingContinuation;
import org.granite.logging.Logger;
import org.granite.osgi.impl.config.IChannel;

import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
@Instantiate
public class AMFMessageServlet extends AbstractGravityServlet {
    private static final Logger log = Logger.getLogger(AMFMessageServlet.class);

    @Requires
    private HttpService httpService;

    @Requires
    private IGravity gravity;

    private HttpContext httpContext;

    private Map<String, String> aliases = new HashMap<String, String>();

    @Validate
    private void starting() {
        log.debug("Gravity's AMFMessageServlet started");

        httpContext = httpService.createDefaultHttpContext();
    }

    @Invalidate
    private void stopping() {
        log.debug("Gravity's AMFMessageServlet stopped");

        // Remove all aliases
        for (Iterator<String> it = aliases.keySet().iterator(); it.hasNext();) {
            String uri = it.next();
            httpService.unregister(uri);
            it.remove();
            log.info("Remove alias: " + uri);
        }
    }

    @Bind(aggregate = true, optional = true)
    private synchronized void bindChannel(final IChannel ichannel) {
        Channel channel = ichannel.getChannel();
        try {
            if (channel.getClassName().equals("org.granite.gravity.channels.GravityChannel")) {

                String uri = aliases.get(channel.getId());

                if (uri == null) {
                    uri = channel.getEndPoint().getUri();

                    synchronized (aliases) {
                        httpService.registerServlet(uri, this, null, httpContext);
                        aliases.put(channel.getId(), uri);
                    }
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
    private synchronized void unbindChannel(final IChannel ichannel) {
        Channel channel = ichannel.getChannel();
        try {
            if (channel.getClassName().equals("org.granite.gravity.channels.GravityChannel")) {

                String uri = aliases.get(channel.getId());

                if (uri != null) {
                    synchronized (aliases) {
                        aliases.remove(channel.getId());
                        httpService.unregister(uri);
                    }
                    log.info("Remove alias: " + uri);
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


    	log.debug("doPost: from %s:%d", request.getRemoteAddr(), request.getRemotePort());

		try {
			// Setup context (thread local GraniteContext, etc.)
			initializeRequest(gravity.getGravity(), request, response);

			AsyncMessage connect = getConnectMessage(request);

			// Resumed request (new received messages or timeout).
			if (connect != null) {
				try {
					String channelId = (String)connect.getClientId();
					GenericChannel channel = (GenericChannel)gravity.getGravity().getChannel(channelId);
					// Reset channel continuation instance and deliver pending messages.
					synchronized (channel) {
						channel.reset();
						channel.runReceived(new AsyncHttpContext(request, response, connect));
					}
				}
				finally {
					removeConnectMessage(request);
				}
				return;
			}

			// New Request.
    			Message[] amf3Requests = deserialize(gravity.getGravity(), request);

            log.debug(">> [AMF3 REQUESTS] %s", (Object)amf3Requests);

            Message[] amf3Responses = null;

            boolean accessed = false;
            for (int i = 0; i < amf3Requests.length; i++) {
                Message amf3Request = amf3Requests[i];

                // Ask gravity to create a specific response (will be null for connect request from tunnel).
                Message amf3Response = gravity.getGravity().handleMessage(amf3Request);
                String channelId = (String)amf3Request.getClientId();

                // Mark current channel (if any) as accessed.
                if (!accessed)
                	accessed = gravity.getGravity().access(channelId);

                // (Re)Connect message from tunnel.
                if (amf3Response == null) {
                    if (amf3Requests.length > 1)
                        throw new IllegalArgumentException("Only one request is allowed on tunnel.");

                	GenericChannel channel = (GenericChannel)gravity.getGravity().getChannel(channelId);
                	if (channel == null)
                		throw new NullPointerException("No channel on tunnel connect");

                    // Try to send pending messages if any (using current container thread).
                	if (!channel.runReceived(new AsyncHttpContext(request, response, amf3Request))) {
                        // No pending messages, wait for new ones or timeout.
	                    setConnectMessage(request, amf3Request);
	                	synchronized (channel) {
	                		WaitingContinuation continuation = new WaitingContinuation(channel);
		                	channel.setContinuation(continuation);
		                	continuation.suspend(gravity.getGravity().getGravityConfig().getLongPollingTimeoutMillis());
	                	}
                	}

                	return;
                }

                if (amf3Responses == null)
                	amf3Responses = new Message[amf3Requests.length];
                amf3Responses[i] = amf3Response;
            }

            log.debug("<< [AMF3 RESPONSES] %s", (Object)amf3Responses);

            serialize(gravity.getGravity(), response, amf3Responses);
		}
        catch (IOException e) {
            log.error(e, "Gravity message error");
            throw e;
        }
        catch (ClassNotFoundException e) {
            log.error(e, "Gravity message error");
            throw new ServletException("Gravity message error", e);
        }
		finally {
			// Cleanup context (thread local GraniteContext, etc.)
			cleanupRequest(request);
		}

        removeConnectMessage(request);
    }
}
