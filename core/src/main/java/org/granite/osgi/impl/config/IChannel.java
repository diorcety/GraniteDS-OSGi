package org.granite.osgi.impl.config;

import org.granite.config.flex.Channel;
import org.granite.config.flex.EndPoint;
import org.granite.util.XMap;

public interface IChannel {
    public String getClassName();

    public EndPoint getEndPoint();

    public String getId();

    public XMap getProperties();

    public boolean isLegacyXmlSerialization() ;

    public boolean isLegacyCollectionSerialization();

    public Channel getChannel();
}
