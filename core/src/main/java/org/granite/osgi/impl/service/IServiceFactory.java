package org.granite.osgi.impl.service;

import flex.messaging.messages.RemotingMessage;
import org.granite.messaging.service.ServiceException;
import org.granite.messaging.service.ServiceExceptionHandler;

public interface IServiceFactory {
    public IServiceInvoker getServiceInstance(RemotingMessage request) throws ServiceException;

    public ServiceExceptionHandler getServiceExceptionHandler();
}
