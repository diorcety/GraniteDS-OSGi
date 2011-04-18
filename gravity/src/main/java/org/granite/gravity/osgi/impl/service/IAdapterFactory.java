package org.granite.gravity.osgi.impl.service;

import flex.messaging.messages.Message;

import org.granite.gravity.adapters.ServiceAdapter;
import org.granite.messaging.service.ServiceException;

public interface IAdapterFactory {
    public ServiceAdapter getServiceAdapter(Message request) throws ServiceException ;

    public ServiceAdapter getServiceAdapter(String messageType, String destinationId) throws ServiceException;
}
