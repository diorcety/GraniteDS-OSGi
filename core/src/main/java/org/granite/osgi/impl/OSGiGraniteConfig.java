package org.granite.osgi.impl;

import org.granite.config.GraniteConfig;
import org.granite.messaging.amf.io.util.ActionScriptClassDescriptor;
import org.granite.messaging.service.MainFactory;
import org.granite.osgi.impl.io.OSGiActionScriptClassDescriptor;
import org.granite.osgi.impl.service.OSGiMainFactory;
import org.xml.sax.SAXException;

import java.io.IOException;

public class OSGiGraniteConfig extends GraniteConfig {

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
}
