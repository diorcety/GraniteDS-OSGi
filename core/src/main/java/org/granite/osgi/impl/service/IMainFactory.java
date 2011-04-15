package org.granite.osgi.impl.service;

import flex.messaging.messages.RemotingMessage;
import org.granite.messaging.service.MainFactory;
import org.granite.messaging.service.ServiceException;
import org.granite.messaging.service.ServiceFactory;

public interface IMainFactory {
    public MainFactory getMainFactory();
}
