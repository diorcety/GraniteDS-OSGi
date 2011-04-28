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
import org.granite.gravity.Gravity;
import org.granite.messaging.amf.io.AMF3Deserializer;
import org.granite.osgi.impl.OSGiGraniteClassUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class OSGiAMF3Deserializer extends AMF3Deserializer {

    public OSGiAMF3Deserializer(InputStream in) {
        super(in);
    }

    @Override
    public Object readObject() throws IOException {
        OSGiGraniteClassUtil.setDestination(null);
        Object obj = super.readObject();
        if (obj instanceof AbstractMessage) {
            AbstractMessage message = (AbstractMessage) obj;
            if (Boolean.TRUE.equals(message.getHeader(Gravity.BYTEARRAY_BODY_HEADER))) {
                byte[] byteArray = (byte[]) message.getBody();
                ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
                AMF3Deserializer deser = new AMF3Deserializer(bais);
                message.setBody(deser.readObject());
            }
            OSGiGraniteClassUtil.setDestination(message.getDestination());
            return resolve(obj);
        }
        return obj;
    }


    public Object resolve(Object obj) {
        if (obj instanceof AbstractMessage) {
            AbstractMessage abstractMessage = (AbstractMessage) obj;
            abstractMessage.setBody(resolve(abstractMessage.getBody()));

        } else if (obj instanceof OSGiDelayedObject) {
            OSGiDelayedObject oi = (OSGiDelayedObject) obj;
            for (String key : oi.keySet()) {
                Object so = oi.get(key);
                so = resolve(so);
                oi.put(key, so);
            }

            try {
                return oi.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Could not instantiate object: " + obj, e);
            }

        } else if (obj instanceof List) {
            List list = (List) obj;
            for (int i = 0; i < list.size(); i++) {
                list.set(i, resolve(list.get(i)));
            }
        } else if (obj != null && obj.getClass() != null && obj.getClass().isArray()) {
            Object objects[] = (Object[]) obj;
            for (int i = 0; i < objects.length; i++) {
                objects[i] = resolve(objects[i]);
            }
        }
        return obj;
    }
}
