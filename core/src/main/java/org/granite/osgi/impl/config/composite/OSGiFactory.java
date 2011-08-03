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

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;
import org.granite.config.flex.ServicesConfig;
import org.granite.config.flex.SimpleFactory;
import org.granite.logging.Logger;
import org.granite.osgi.GraniteConstants;
import org.granite.util.XMap;

@Component(name = GraniteConstants.FACTORY)
@Provides
public class OSGiFactory extends SimpleFactory {

    private static final Logger log = Logger.getLogger(OSGiFactory.class);

    @Requires(proxy = false)
    private ServicesConfig servicesConfig;

    @ServiceProperty(name = "ID")
    private String ID;

    //
    private boolean started = false;

    //
    protected OSGiFactory() {
        super(null, null, XMap.EMPTY_XMAP);
    }

    @Property(name = "id", mandatory = true)
    private void setId(String id) {
        this.id = id;
        this.ID = id;
    }

    @Validate
    public void start() {
        log.debug("Start Factory: " + toString());
        if (servicesConfig.findFactoryById(id) == null) {
            servicesConfig.addFactory(this);
            started = true;
        } else {
            log.error("Factory \"" + id + "\" already registered");
        }
    }

    @Invalidate
    public void stop() {
        log.debug("Stop Factory: " + toString());

        if (started) {
            servicesConfig.removeFactory(id);
            started = false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OSGiFactory that = (OSGiFactory) o;

        if (this != that) return false;

        return true;
    }

    @Override
    public String toString() {
        return "Factory{" +
                "id=" + id +
                '}';
    }
}
