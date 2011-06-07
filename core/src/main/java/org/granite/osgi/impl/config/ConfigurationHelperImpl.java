package org.granite.osgi.impl.config;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.granite.logging.Logger;
import org.granite.osgi.ConfigurationHelper;

import java.util.Dictionary;
import java.util.Hashtable;

@Component
@Instantiate
@Provides
public class ConfigurationHelperImpl implements ConfigurationHelper {

    private static final Logger log = Logger.getLogger(ConfigurationHelperImpl.class);

    @Requires(from = "org.granite.osgi.impl.config.OSGiService")
    Factory serviceBuilder;
    @Requires(from = "org.granite.osgi.impl.config.OSGiServiceWithAdapter")
    Factory serviceWithAdapterBuilder;
    @Requires(from = "org.granite.osgi.impl.config.OSGiAdapter")
    Factory adapterBuilder;
    @Requires(from = "org.granite.osgi.impl.config.OSGiFactory")
    Factory factoryBuilder;
    @Requires(from = "org.granite.osgi.impl.config.OSGiGraniteChannel")
    Factory graniteChannelBuilder;
    @Requires(from = "org.granite.osgi.impl.config.OSGiGravityChannel")
    Factory gravityChannelBuilder;
    @Requires(from = "org.granite.osgi.impl.config.OSGiDestination")
    Factory destinationBuilder;
    @Requires(from = "org.granite.osgi.impl.config.OSGiDestinationWithAdapter")
    Factory destinationWithAdapterBuilder;
    @Requires(from = "org.granite.osgi.impl.config.OSGiDestinationWithFactory")
    Factory destinationWithFactoryBuilder;

    private ConfigurationHelperImpl() {

    }

    public ComponentInstance newAdapter(String id) {
        try {
            Dictionary properties = new Hashtable();
            properties.put("ID", id);
            return adapterBuilder.createComponentInstance(properties);
        } catch (Exception e) {
            log.error(e, "Can't create the adapter: " + id);
            return null;
        }
    }

    public ComponentInstance newFactory(String id) {
        try {
            Dictionary properties = new Hashtable();
            properties.put("ID", id);
            return factoryBuilder.createComponentInstance(properties);
        } catch (Exception e) {
            log.error(e, "Can't create the adapter: " + id);
            return null;
        }
    }

    public ComponentInstance newGraniteDestination(String id, String service) {
        try {
            Dictionary filters = new Hashtable();
            filters.put("service", "(ID=" + service + ")");

            Dictionary properties = new Hashtable();
            properties.put("ID", id);
            properties.put("requires.filters", filters);
            return destinationBuilder.createComponentInstance(properties);
        } catch (Exception e) {
            log.error(e, "Can't create the service: " + id);
            return null;
        }
    }

    public ComponentInstance newGraniteDestination(String id, String service, String factory, SCOPE scope) {
        try {
            Dictionary filters = new Hashtable();
            filters.put("service", "(ID=" + service + ")");
            filters.put("factory", "(ID=" + factory + ")");
            Dictionary properties = new Hashtable();
            properties.put("ID", id);
            properties.put("SCOPE", scope.toString());
            properties.put("requires.filters", filters);
            return destinationWithFactoryBuilder.createComponentInstance(properties);
        } catch (Exception e) {
            log.error(e, "Can't create the service: " + id);
            return null;
        }
    }

    public ComponentInstance newGravityDestination(String id, String service) {
        try {
            Dictionary filters = new Hashtable();
            filters.put("service", "(ID=" + service + ")");

            Dictionary properties = new Hashtable();
            properties.put("ID", id);
            properties.put("requires.filters", filters);
            return destinationBuilder.createComponentInstance(properties);
        } catch (Exception e) {
            log.error(e, "Can't create the service: " + id);
            return null;
        }
    }

    public ComponentInstance newGravityDestination(String id, String service, String adapter) {
        try {
            Dictionary filters = new Hashtable();
            filters.put("service", "(ID=" + service + ")");
            filters.put("adapter", "(ID=" + adapter + ")");
            Dictionary properties = new Hashtable();
            properties.put("ID", id);
            properties.put("requires.filters", filters);

            return destinationWithAdapterBuilder.createComponentInstance(properties);

        } catch (Exception e) {
            log.error(e, "Can't create the service: " + id);
            return null;
        }
    }

    public ComponentInstance newGraniteService(String id) {
        try {
            Dictionary properties = new Hashtable();
            properties.put("ID", id);
            properties.put("MESSAGETYPES", "flex.messaging.messages.RemotingMessage");
            return serviceBuilder.createComponentInstance(properties);
        } catch (Exception e) {
            log.error(e, "Can't create the service: " + id);
            return null;
        }
    }

    public ComponentInstance newGravityService(String id) {
        try {
            Dictionary properties = new Hashtable();
            properties.put("ID", id);
            properties.put("MESSAGETYPES", "flex.messaging.messages.AsyncMessage");
            return serviceBuilder.createComponentInstance(properties);

        } catch (Exception e) {
            log.error(e, "Can't create the service: " + id);
            return null;
        }
    }

    public ComponentInstance newGravityService(String id, String defaultAdapter) {
        try {
            Dictionary filters = new Hashtable();

            Dictionary properties = new Hashtable();
            properties.put("ID", id);
            properties.put("MESSAGETYPES", "flex.messaging.messages.AsyncMessage");
            properties.put("requires.filters", filters);
            filters.put("defaultAdapter", "(ID=" + defaultAdapter + ")");
            return serviceWithAdapterBuilder.createComponentInstance(properties);

        } catch (Exception e) {
            log.error(e, "Can't create the service: " + id);
            return null;
        }
    }

    public ComponentInstance newGraniteChannel(String id, String uri, String context) {
        try {
            Dictionary properties = new Hashtable();
            properties.put("ID", id);
            properties.put("CONTEXT", context);
            properties.put("CLASS", "mx.messaging.channels.AMFChannel");
            properties.put("ENDPOINT_URI", uri);
            return graniteChannelBuilder.createComponentInstance(properties);
        } catch (Exception e) {
            log.error(e, "Can't create the channel: " + id);
            return null;
        }
    }

    public ComponentInstance newGravityChannel(String id, String uri, String context) {
        try {
            Dictionary properties = new Hashtable();
            properties.put("ID", id);
            properties.put("CONTEXT", context);
            properties.put("CLASS", "org.granite.gravity.channels.GravityChannel");
            properties.put("ENDPOINT_URI", uri);
            return gravityChannelBuilder.createComponentInstance(properties);
        } catch (Exception e) {
            log.error(e, "Can't create the channel: " + id);
            return null;
        }
    }
}
