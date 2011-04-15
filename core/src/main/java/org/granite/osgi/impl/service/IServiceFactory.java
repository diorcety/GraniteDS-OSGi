package org.granite.osgi.impl.service;

import flex.messaging.messages.RemotingMessage;
import org.granite.messaging.service.ServiceException;
import org.granite.messaging.service.ServiceExceptionHandler;
import org.granite.messaging.service.ServiceFactory;

public interface IServiceFactory {
    public ServiceFactory getServiceFactory();
}
