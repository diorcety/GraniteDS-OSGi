package org.granite.osgi.impl.io;

import flex.messaging.messages.Message;
import org.granite.messaging.amf.process.AMF3MessageInterceptor;

public class OSGiAMF3MessageInterceptor implements AMF3MessageInterceptor {
    @Override
    public void before(Message request) {
    }

    @Override
    public void after(Message request, Message response) {
        if (request != null && response != null)
            response.setDestination(request.getDestination());
    }
}
