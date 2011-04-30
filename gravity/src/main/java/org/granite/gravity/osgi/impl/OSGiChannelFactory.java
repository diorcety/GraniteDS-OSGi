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


package org.granite.gravity.osgi.impl;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import org.granite.context.GraniteContext;
import org.granite.gravity.AbstractChannelFactory;
import org.granite.gravity.Channel;

@Component
@Instantiate
@Provides
public class OSGiChannelFactory extends AbstractChannelFactory {
    @Requires
    GraniteContext graniteContext;

    @Override
    public Channel newChannel(String id) {
        return new OSGiChannel(graniteContext, getServletConfig(), getGravityConfig(), id);
    }
}
