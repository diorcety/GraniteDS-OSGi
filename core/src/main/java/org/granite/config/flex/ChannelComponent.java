package org.granite.config.flex;

import org.granite.util.XMap;

public interface ChannelComponent {
    public String getClassName();

    public EndPoint getEndPoint();

    public String getId();

    public XMap getProperties();

    public Channel getChannel();
}
