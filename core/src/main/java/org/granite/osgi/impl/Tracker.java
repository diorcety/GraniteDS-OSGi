package org.granite.osgi.impl;

import org.apache.felix.ipojo.ComponentInstance;
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

@Component
@Instantiate
public class Tracker {

    private static final Logger LOG = Logger.getLogger(Tracker.class);

    @Requires(from = "org.granite.config.flex.Destination")
    Factory destinationFactory;

    @Requires(from = "org.granite.config.flex.Factory")
    Factory factoryFactory;

    @Requires
    OSGiBase osgiBase;

    private static Map<String, ServiceDestination> destinationMap =
            new HashMap<String, ServiceDestination>();
    private static Map<String, ServiceFactory> factoryMap =
            new HashMap<String, ServiceFactory>();

    @Validate
    private void starting() {
        LOG.debug("GraniteDS Tracker started");

    }

    @Invalidate
    private void stopping() {
        LOG.debug("GraniteDS Tracker stopped");
    }

    @Bind(aggregate = true, optional = true)
    public final synchronized void bindDestination(
            final ServiceDestination destination) {
        destinationMap.put(destination.getId(), destination);
        osgiBase.registerClass(destination.getClass());
    }

    @Unbind
    public final synchronized void unbindDestination(
            final ServiceDestination destination) {
        destinationMap.remove(destination.getId());
        osgiBase.unregisterClass(destination.getClass());
    }

    @Bind(aggregate = true, optional = true)
    public final synchronized void bindFactory(
            final ServiceFactory factory) {
        factoryMap.put(factory.getId(), factory);
        osgiBase.registerClass(factory.getClass());
    }

    @Unbind
    public final synchronized void unbindFactory(
            final ServiceFactory factory) {
        factoryMap.remove(factory.getId());
        osgiBase.unregisterClass(factory.getClass());
    }

    public static synchronized ServiceFactory getFactory(String id) {
        return factoryMap.get(id);
    }

    public static synchronized ServiceDestination getDestination(String id) {
        return destinationMap.get(id);
    }

}
