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

package org.granite.gravity.osgi.impl.service;

import flex.messaging.messages.AsyncMessage;
import flex.messaging.messages.CommandMessage;

import org.granite.config.flex.Adapter;

import org.granite.gravity.Channel;
import org.granite.gravity.adapters.ServiceAdapter;
import org.granite.logging.Logger;
import org.granite.osgi.service.GraniteAdapter;


public class OSGiAdapterAbstraction extends ServiceAdapter {

    private static final Logger log = Logger.getLogger(OSGiAdapterAbstraction.class);

    private final GraniteAdapter graniteAdapter;
    private Adapter adapter;

    OSGiAdapterAbstraction(GraniteAdapter graniteAdapter, Adapter adapter) {
        this.graniteAdapter = graniteAdapter;
        this.adapter = adapter;
    }

    public Adapter getAdapter() {
        return adapter;
    }

    @Override
    public Object manage(Channel fromClient, CommandMessage message) {
        return graniteAdapter.manage(fromClient,message);
    }

    @Override
    public Object invoke(Channel fromClient, AsyncMessage message) {
        return graniteAdapter.invoke(fromClient, message);
    }
}
