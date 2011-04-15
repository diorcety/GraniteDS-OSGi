package org.granite.osgi.impl.config;

import org.granite.config.flex.Destination;
import org.granite.config.flex.Adapter;
import org.granite.messaging.service.security.DestinationSecurizer;
import org.granite.util.XMap;

import java.util.List;

public interface IDestination {
    public Destination getDestination();
}
