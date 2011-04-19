package org.granite.gravity.osgi.impl;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import org.granite.context.GraniteContext;
import org.granite.gravity.Channel;
import org.granite.gravity.generic.GenericChannelFactory;


@Component
@Instantiate
@Provides
public class OSGiChannelFactory extends GenericChannelFactory {
    @Requires
    GraniteContext graniteContext;

    @Override
    public Channel newChannel(String id) {
        return new OSGiChannel(graniteContext, getServletConfig(), getGravityConfig(), id);
    }
}
