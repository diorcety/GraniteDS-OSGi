package org.granite.osgi.impl.config;

import org.granite.config.flex.Destination;
import org.granite.config.flex.Adapter;
import org.granite.messaging.service.security.DestinationSecurizer;
import org.granite.util.XMap;

import java.util.List;

public interface IDestination {
    public Adapter getAdapter();

    public List<String> getChannelRefs();

    public String getId();

    public XMap getProperties();

    public List<String> getRoles();

    public Class<?> getScannedClass();

    public DestinationSecurizer getSecurizer();

    public boolean isSecured();

    public Destination getDestination();
}
