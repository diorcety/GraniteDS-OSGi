package org.granite.config.flex;

import org.granite.util.XMap;

public interface ChannelInterface {
    public String getClassName();

    public EndPoint getEndPoint();

    public String getId();

    public XMap getProperties();
}
