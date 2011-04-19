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

package org.granite.gravity.osgi.impl.config;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.gravity.ChannelFactory;
import org.granite.gravity.GravityConfig;
import org.granite.gravity.SimpleGravityConfig;
import org.granite.logging.Logger;

import org.xml.sax.SAXException;

import java.io.IOException;

@Component
@Instantiate
@Provides
public class OSGiGravityConfig extends SimpleGravityConfig {

    @Requires
    ChannelFactory channelFactory;

    private static final Logger log = Logger.getLogger(OSGiGravityConfig.class);

    OSGiGravityConfig() throws IOException, SAXException {
        super(null, null);
    }

    public GravityConfig getGravityConfig() {
        return this;
    }

    @Override
    public ChannelFactory getChannelFactory() {
        return channelFactory;
    }

    @Validate
    public void starting() {
        log.debug("Start GravityConfig");
    }

    @Invalidate
    public void stopping() {
        log.debug("Stop GravityConfig");
    }
}

