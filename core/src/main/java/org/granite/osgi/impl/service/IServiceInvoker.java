package org.granite.osgi.impl.service;

import flex.messaging.messages.RemotingMessage;
import org.granite.messaging.service.ServiceException;

public interface IServiceInvoker {
    public Object invoke(RemotingMessage request) throws ServiceException;
}
