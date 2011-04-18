package org.granite.osgi.service;

import flex.messaging.messages.AsyncMessage;
import flex.messaging.messages.CommandMessage;
import org.granite.gravity.Channel;

public interface GraniteAdapter {
    public String getId();

    public Object manage(Channel fromClient, CommandMessage message);

    public Object invoke(Channel fromClient, AsyncMessage message);
}
