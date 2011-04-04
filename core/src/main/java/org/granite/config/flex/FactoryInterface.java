package org.granite.config.flex;

import org.granite.util.XMap;

public interface FactoryInterface {
    public String getClassName();

    public String getId();

    public XMap getProperties();
}
