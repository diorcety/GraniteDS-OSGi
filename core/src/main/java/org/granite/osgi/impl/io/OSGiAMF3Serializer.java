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

package org.granite.osgi.impl.io;


import flex.messaging.messages.AbstractMessage;
import flex.messaging.messages.CommandMessage;

import org.granite.messaging.amf.io.AMF3Serializer;
import org.granite.osgi.impl.OSGiConstants;
import org.granite.osgi.impl.OSGiGraniteClassUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class OSGiAMF3Serializer extends AMF3Serializer {
    public OSGiAMF3Serializer(OutputStream out) {
        super(out);
    }

    public OSGiAMF3Serializer(OutputStream out, boolean warnOnChannelMissing) {
        super(out, warnOnChannelMissing);
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        if (obj instanceof AbstractMessage && ! (obj instanceof CommandMessage)) {
            AbstractMessage abstractMessage = (AbstractMessage) obj;
            OSGiGraniteClassUtil.setDestination(abstractMessage.getDestination());
            abstractMessage.setHeader(OSGiConstants.BYTEARRAY_BODY_HEADER, Boolean.TRUE.toString());
            ByteArrayOutputStream bais = new ByteArrayOutputStream();
            AMF3Serializer ser = new AMF3Serializer(bais);
            ser.writeObject(abstractMessage.getBody());
            abstractMessage.setBody(bais.toByteArray());
        }
        super.writeObject(obj);
    }
}
