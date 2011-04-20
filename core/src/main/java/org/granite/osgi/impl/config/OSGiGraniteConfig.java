/*
  GRANITE DATA SERVICES
  Copyright (C) 2011 GRANITE DATA SERVICES S.A.S.

  This file is part of Granite Data Services.

  Granite Data Services is free software; you can redistribute it and/or modify
  it under the terms of the GNU Library General Public License as published by
  the Free Software Foundation; either version 2 of the License, or (at your
  option) any later version.

  Granite Data Services is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Library General Public License
  for more details.

  You should have received a copy of the GNU Library General Public License
  along with this library; if not, see <http://www.gnu.org/licenses/>.
*/

package org.granite.osgi.impl.config;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.SimpleGraniteConfig;
import org.granite.logging.Logger;
import org.granite.messaging.amf.io.util.ActionScriptClassDescriptor;
import org.granite.messaging.amf.process.AMF3MessageInterceptor;
import org.granite.osgi.impl.io.OSGiAMF3Deserializer;
import org.granite.osgi.impl.io.OSGiAMF3MessageInterceptor;
import org.granite.osgi.impl.io.OSGiAMF3Serializer;
import org.granite.osgi.impl.io.OSGiActionScriptClassDescriptor;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;

@Component
@Instantiate
@Provides
public class OSGiGraniteConfig extends SimpleGraniteConfig {

    private static final Logger log = Logger.getLogger(OSGiGraniteConfig.class);

    private OSGiAMF3MessageInterceptor interceptor = new OSGiAMF3MessageInterceptor();

    //
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

    @Validate
    public void starting() {
        log.debug("Start GraniteConfig");
    }

    @Invalidate
    public void stopping() {
        log.debug("Stop GraniteConfig");
    }
}

