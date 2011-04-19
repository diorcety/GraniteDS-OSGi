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

package org.granite.osgi.impl;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.config.GraniteConfig;
import org.granite.config.flex.ServicesConfig;
import org.granite.context.AMFContext;
import org.granite.context.AMFContextImpl;
import org.granite.context.GraniteContext;
import org.granite.logging.Logger;
import org.granite.messaging.service.MainFactory;

import java.util.HashMap;
import java.util.Map;

@Component
@Provides
@Instantiate
public class OSGiGraniteContext implements GraniteContext {

    private static final Logger log = Logger.getLogger(OSGiGraniteContext.class);


    protected Map<String, Object> applicationMap = null;
    private final AMFContext amfContext;

    @Requires
    private ServicesConfig servicesConfig;

    @Requires
    private GraniteConfig graniteConfig;

    @Requires
    private MainFactory mainFactory;

    @Requires
    private OSGiGraniteClassLoader classLoader;

    private OSGiGraniteContext() {
        this.amfContext = new AMFContextImpl();
    }

    @Validate
    public void starting() {
        log.debug("Start GraniteContext");
    }

    @Invalidate
    public void stopping() {
        log.debug("Stop GraniteContext");
    }

    @Override
    public ServicesConfig getServicesConfig() {
        return servicesConfig;
    }

    @Override
    public GraniteConfig getGraniteConfig() {
        return graniteConfig;
    }

    @Override
    public AMFContext getAMFContext() {
        return amfContext;
    }

    @Override
    public Object getSessionLock() {
        return null;
    }

    @Override
    public Map<String, String> getInitialisationMap() {
        return null;
    }

    @Override
    public Map<String, Object> getApplicationMap() {
        if (applicationMap == null)
            applicationMap = new HashMap<String, Object>();
        return applicationMap;
    }

    @Override
    public Map<String, Object> getSessionMap(boolean create) {
        return null;
    }

    @Override
    public Map<String, Object> getSessionMap() {
        return null;
    }

    @Override
    public Map<String, Object> getRequestMap() {
        return null;
    }

    @Override
    public Class<?> forName(String type) throws ClassNotFoundException {
        return classLoader.forName(type);
    }

    @Override
    public MainFactory getMainFactory() {
        return mainFactory;
    }
}
