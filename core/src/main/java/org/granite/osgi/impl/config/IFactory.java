package org.granite.osgi.impl.config;

import org.granite.config.flex.Factory;
import org.granite.util.XMap;

public interface IFactory {
    public String getClassName();

    public String getId();

    public XMap getProperties();

    public Factory getFactory();
}
