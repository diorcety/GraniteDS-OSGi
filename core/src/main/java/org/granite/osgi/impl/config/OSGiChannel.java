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
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.flex.*;
import org.granite.logging.Logger;
import org.granite.util.XMap;

import java.util.Map;

@Component(name = "org.granite.config.flex.Channel")
@Provides
public class OSGiChannel extends SimpleChannel {

    private static final Logger log = Logger.getLogger(OSGiChannel.class);

    @Requires
    private ServicesConfig servicesConfig;

    public String ENDPOINT_URI;

    public String ENDPOINT_CLASS;

    protected OSGiChannel() {
        super(null, null, null, XMap.EMPTY_XMAP);
    }

    @Validate
    public void starting() {
        start();
    }

    @Invalidate
    public void stopping() {
        stop();
    }

    @Property(name = "ID", mandatory = true)
    private void setId(String id) {
        this.id = id;
    }

    @Property(name = "CLASS", mandatory = false,
              value = "mx.messaging.channels.AMFChannel")
    private void setClass(String className) {
        this.className = className;
    }

    @Property(name = "ENDPOINT_URI", mandatory = true)
    private void setEndPointURI(String epURI) {
        this.ENDPOINT_URI = epURI;
        this.endPoint = new SimpleEndPoint(ENDPOINT_URI, ENDPOINT_CLASS);
    }

    @Property(name = "ENDPOINT_CLASS", mandatory = false,
              value = "flex.messaging.endpoints.AMFEndpoint")
    private void setEndPointClass(String epClass) {
        this.ENDPOINT_CLASS = epClass;
        this.endPoint = new SimpleEndPoint(ENDPOINT_URI, ENDPOINT_CLASS);
    }


    @Property(name = "PROPERTIES", mandatory = false)
    private void setProperties(Map<String, String> properties) {
        this.properties = new XMap(properties);
    }

    public void start() {
        log.debug("Start Channel: " + this.id);
        servicesConfig.addChannel(this);
    }

    public void stop() {
        log.debug("Stop Channel: " + this.id);
        if (servicesConfig != null) {
            servicesConfig.removeChannel(this.id);
        }
    }

    public Channel getChannel() {
        return this;
    }
}
