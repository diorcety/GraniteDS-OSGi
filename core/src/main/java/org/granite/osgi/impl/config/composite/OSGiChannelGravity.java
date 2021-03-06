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
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;
import org.granite.config.flex.ServicesConfig;
import org.granite.config.flex.SimpleChannel;
import org.granite.config.flex.SimpleEndPoint;
import org.granite.logging.Logger;
import org.granite.util.XMap;

import java.util.Dictionary;
import java.util.Hashtable;

@Component
@Provides
public class OSGiChannelGravity extends SimpleChannel {

    private static final Logger log = Logger.getLogger(OSGiChannelGravity.class);

    @Requires(proxy = false)
    private ServicesConfig servicesConfig;

    @ServiceProperty(name = "ID")
    private String ID;

    @Requires(from = "org.granite.gravity.osgi.impl.OSGiGravityMessageServlet")
    Factory servletBuilder;

    private boolean started = false;

    private ComponentInstance servlet;

    //
    protected OSGiChannelGravity() {
        super(null, "org.granite.gravity.channels.GravityChannel", null, XMap.EMPTY_XMAP);
    }

    @Property(name = "id", mandatory = true)
    private void setId(String id) {
        this.id = id;
        this.ID = id;
    }

    @Property(name = "context", mandatory = true)
    private String context;

    @Property(name = "uri", mandatory = true)
    private void setEndPointURI(String epURI) {
        this.endPoint = new SimpleEndPoint(epURI, "flex.messaging.endpoints.AMFEndpoint");
    }

    @Validate
    public void start() {
        log.debug("Start OSGiChannelGravity: " + toString());

        if (servicesConfig.findChannelById(id) == null) {
            try {
                Dictionary properties = new Hashtable();
                properties.put("URI", endPoint.getUri());
                Dictionary filters = new Hashtable();
                filters.put("context", "(ID=" + context + ")");
                properties.put("requires.filters", filters);

                servlet = servletBuilder.createComponentInstance(properties);

                servicesConfig.addChannel(this);
                started = true;
            } catch (Exception e) {
                log.error("Can't create the servlet for \"" + id + "\"");
            }
        } else {
            log.error("Channel \"" + id + "\" already registered");
        }
    }

    @Invalidate
    public void stop() {
        log.debug("Stop OSGiChannelGravity: " + toString());
        if (started) {
            servlet.dispose();
            servicesConfig.removeChannel(id);
            started = false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OSGiChannelGravity that = (OSGiChannelGravity) o;

        if (this != that) return false;

        return true;
    }

    @Override
    public String toString() {
        return "GraniteChannel{" +
                "id=" + id +
                ", uri=" + endPoint.getUri() +
                ", context=" + context +
                ", gravity=true" +
                '}';
    }
}
