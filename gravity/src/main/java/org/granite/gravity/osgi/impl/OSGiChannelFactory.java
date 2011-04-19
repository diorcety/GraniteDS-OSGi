package org.granite.gravity.osgi.impl;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.granite.gravity.Channel;
import org.granite.gravity.generic.GenericChannel;
import org.granite.gravity.generic.GenericChannelFactory;
import org.granite.osgi.impl.IGraniteContext;


@Component
@Instantiate
@Provides
public class OSGiChannelFactory extends GenericChannelFactory {
    @Requires
    IGraniteContext graniteContext;

    @Override
    public Channel newChannel(String id) {
        return new OSGiChannel(graniteContext.getGraniteContext(), getServletConfig(), getGravityConfig(), id);
    }
}
