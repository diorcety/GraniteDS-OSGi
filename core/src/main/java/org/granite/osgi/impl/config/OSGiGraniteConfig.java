package org.granite.osgi.impl.config;

import org.apache.felix.ipojo.annotations.*;
import org.granite.config.GraniteConfig;
import org.granite.config.SimpleGraniteConfig;
import org.granite.logging.Logger;
import org.granite.messaging.amf.io.util.ActionScriptClassDescriptor;
import org.granite.messaging.amf.process.AMF3MessageInterceptor;
import org.granite.osgi.impl.io.OSGiAMF3Deserializer;
import org.granite.osgi.impl.io.OSGiAMF3MessageInterceptor;
import org.granite.osgi.impl.io.OSGiAMF3Serializer;
import org.granite.osgi.impl.io.OSGiActionScriptClassDescriptor;
import org.xml.sax.SAXException;

import java.io.*;

@Component
@Instantiate
@Provides
public class OSGiGraniteConfig extends SimpleGraniteConfig {

    private static final Logger log = Logger.getLogger(OSGiGraniteConfig.class);

    private OSGiAMF3MessageInterceptor interceptor = new OSGiAMF3MessageInterceptor();

    OSGiGraniteConfig() throws IOException, SAXException {
        super(null, null, null, null);
    }

    public Class<? extends ActionScriptClassDescriptor> getActionScriptDescriptor(String type) {
        try {
            return super.getActionScriptDescriptor(type);
        } catch (Exception e) {
            return OSGiActionScriptClassDescriptor.class;
        }
    }

    @Override
    public AMF3MessageInterceptor getAmf3MessageInterceptor() {
        return interceptor;
    }

    @Override
    public ObjectInput newAMF3Deserializer(InputStream in) {
        return new OSGiAMF3Deserializer(in);
    }

    @Override
    public ObjectOutput newAMF3Serializer(OutputStream out) {
        return new OSGiAMF3Serializer(out);
    }

    public GraniteConfig getGraniteConfig() {
        return this;
    }

    @Validate
    public void starting() {
        log.debug("Start GraniteConfig");
    }

    @Invalidate
    public void stopping() {
        log.debug("Stop GraniteConfig");
    }
}

