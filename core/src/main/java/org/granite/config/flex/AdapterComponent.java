package org.granite.config.flex;

import org.granite.util.XMap;

public interface AdapterComponent {

    public String getClassName();

    public String getId();

    public XMap getProperties();

    public Adapter getAdapter();
}
