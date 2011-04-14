package org.granite.osgi.impl.config;

import org.granite.config.flex.Adapter;
import org.granite.util.XMap;

public interface IAdapter {

    public String getClassName();

    public String getId();

    public XMap getProperties();

    public Adapter getAdapter();
}
