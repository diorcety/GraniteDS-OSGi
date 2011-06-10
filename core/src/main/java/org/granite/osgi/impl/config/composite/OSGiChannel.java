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

package org.granite.osgi.impl.config.composite;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.granite.logging.Logger;

import java.util.Dictionary;
import java.util.Hashtable;

@Component(name = "org.granite.config.flex.Channel")
public class OSGiChannel {
    private static final Logger log = Logger.getLogger(OSGiChannel.class);

    @Property(name = "id", mandatory = true)
    String id;

    @Property(name = "uri", mandatory = true)
    String uri;

    @Property(name = "context", mandatory = true)
    String context;

    @Property(name = "gravity", value = "false", mandatory = false)
    boolean gravity;

    @Requires(from = "org.granite.osgi.impl.config.composite.OSGiChannelGranite")
    private Factory graniteBuilder;

    @Requires(from = "org.granite.osgi.impl.config.composite.OSGiChannelGravity")
    private Factory gravityBuilder;

    private ComponentInstance instance;

    @Validate
    public void start() {
        try {
            if (!gravity) {
                Dictionary properties = new Hashtable();
                properties.put("id", id);
                properties.put("uri", uri);
                properties.put("context", context);
                instance = graniteBuilder.createComponentInstance(properties);
            } else {
                Dictionary properties = new Hashtable();
                properties.put("id", id);
                properties.put("uri", uri);
                properties.put("context", context);
                instance = gravityBuilder.createComponentInstance(properties);
            }
        } catch (Exception e) {
            log.error(e, "Invalid Channel configuration");
        }
    }

    @Invalidate
    public void stop() {
        instance.dispose();
    }
}
