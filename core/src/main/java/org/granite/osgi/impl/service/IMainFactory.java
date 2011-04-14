package org.granite.osgi.impl.service;

import flex.messaging.messages.RemotingMessage;
import org.granite.messaging.service.ServiceException;

public interface IMainFactory {
    public IServiceFactory getFactoryInstance(RemotingMessage request) throws ServiceException;
}
