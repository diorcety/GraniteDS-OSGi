package org.granite.gravity.osgi.impl;

import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.logging.Logger;
import org.granite.osgi.OSGiBase;
import org.granite.osgi.service.ServiceAdapter;
import org.granite.osgi.service.ServiceDestination;
import org.granite.osgi.service.ServiceFactory;

import java.util.HashMap;
import java.util.Map;

@Component(immediate = true, architecture = true)
@Instantiate
public class Tracker {

    private static final Logger LOG = Logger.getLogger(Tracker.class);

    @Requires(from = "org.granite.config.flex.Adapter")
    Factory adapterFactory;

    @Requires
    OSGiBase osgiBase;

    private static Map<String, ServiceAdapter> adapterMap =
            new HashMap<String, ServiceAdapter>();

    @Validate
    private void starting() {
        LOG.debug("Gravity Tracker started");

    }

    @Invalidate
    private void stopping() {
        LOG.debug("Gravity Tracker stopped");
    }

    @Bind(aggregate = true, optional = true)
    public final synchronized void bindAdapter(
            final ServiceAdapter adapter) {
        adapterMap.put(adapter.getId(), adapter);
        osgiBase.registerClass(adapter.getClass());
    }

    @Unbind
    public final synchronized void unbindAdapter(
            final ServiceAdapter adapter) {
        adapterMap.remove(adapter.getId());
        osgiBase.unregisterClass(adapter.getClass());
    }

    public static synchronized ServiceAdapter getAdapter(String id) {
        return adapterMap.get(id);
    }
}
