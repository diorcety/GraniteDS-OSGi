package org.granite.osgi;

import org.apache.felix.ipojo.ComponentInstance;


public interface ConfigurationHelper {
        public enum SCOPE {
        REQUEST("request"),
        SESSION("session"),
        APPLICATION("application");
        private final String value;

        SCOPE(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    ComponentInstance newAdapter(String id);

    ComponentInstance newFactory(String id);

    ComponentInstance newGraniteDestination(String id, String service);

    ComponentInstance newGravityDestination(String id, String service);

    ComponentInstance newGraniteDestination(String id, String service, String factory, SCOPE scope);

    ComponentInstance newGravityDestination(String id, String service, String adapter);

    ComponentInstance newGraniteService(String id);

    ComponentInstance newGravityService(String id);

    ComponentInstance newGravityService(String id, String defaultAdapter);

    ComponentInstance newGraniteChannel(String id, String uri);

    ComponentInstance newGravityChannel(String id, String uri);
}
