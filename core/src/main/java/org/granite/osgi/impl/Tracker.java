package org.granite.osgi.impl;

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
import org.granite.osgi.service.GraniteDestination;
import org.granite.osgi.service.GraniteFactory;

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

    private static Map<String, GraniteDestination> destinationMap =
            new HashMap<String, GraniteDestination>();
    private static Map<String, GraniteFactory> factoryMap =
            new HashMap<String, GraniteFactory>();

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
            final GraniteDestination destination) {
        osgiBase.registerClass(destination.getClass());
    }

    @Unbind
    public final synchronized void unbindDestination(
            final GraniteDestination destination) {
        osgiBase.unregisterClass(destination.getClass());
    }

    @Bind(aggregate = true, optional = true)
    public final synchronized void bindFactory(
            final GraniteFactory factory) {
        osgiBase.registerClass(factory.getClass());
    }

    @Unbind
    public final synchronized void unbindFactory(
            final GraniteFactory factory) {
        osgiBase.unregisterClass(factory.getClass());
    }

    public static synchronized GraniteFactory getFactory(String id) {
        return factoryMap.get(id);
    }

    public static synchronized GraniteDestination getDestination(String id) {
        return destinationMap.get(id);
    }

}
