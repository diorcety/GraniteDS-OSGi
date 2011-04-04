package org.granite.config.flex;

import org.granite.util.XMap;

import java.util.List;

public interface DestinationInterface {
    public Adapter getAdapter();

    public List<String> getChannelRefs();

    public String getId();

    public XMap getProperties();

    public List<String> getRoles();
}
