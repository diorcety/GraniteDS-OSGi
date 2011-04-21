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

import org.apache.felix.ipojo.annotations.*;

import org.granite.config.flex.ServicesConfig;
import org.granite.config.flex.SimpleFactory;
import org.granite.logging.Logger;
import org.granite.util.XMap;

import java.util.Map;

@Component(name = "org.granite.config.flex.Factory")
@Provides
public class OSGiFactory extends SimpleFactory {

    private static final Logger log = Logger.getLogger(OSGiFactory.class);

    @Requires
    private ServicesConfig servicesConfig;

    //
    private boolean started = false;

    private int version = 0;

    //
    protected OSGiFactory() {
        super(null, null, XMap.EMPTY_XMAP);
    }

    @Validate
    public void starting() {
        started = true;
    }

    @Invalidate
    public void stopping() {
        started = false;
    }

    @Updated
    public void updated() {
        if (started) {
            stop();
        }
        version++;
        start();
    }

    @Property(name = "ID", mandatory = true)
    private void setId(String id) {
        this.id = id;
    }

    public void start() {
        log.debug("Start Factory: " + this.id);
        servicesConfig.addFactory(this);
    }

    public void stop() {
        log.debug("Stop Factory: " + this.id);
        if (servicesConfig != null) {
            servicesConfig.removeFactory(this.id);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OSGiFactory that = (OSGiFactory) o;

        if (this != that || version != that.version) return false;

        return true;
    }
}
