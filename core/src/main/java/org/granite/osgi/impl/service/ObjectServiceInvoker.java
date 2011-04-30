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

package org.granite.osgi.impl.service;

import org.granite.config.flex.Destination;
import org.granite.logging.Logger;
import org.granite.messaging.service.ServiceException;
import org.granite.messaging.service.ServiceFactory;
import org.granite.messaging.service.ServiceInvoker;

public class ObjectServiceInvoker<T extends ServiceFactory> extends ServiceInvoker<T> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(ObjectServiceInvoker.class);

    protected ObjectServiceInvoker(Destination destination, T factory, Object obj) throws ServiceException {
        super(destination, factory);
        this.invokee = obj;
    }

    public Destination getDestination()
    {
        return this.destination;
    }
}
