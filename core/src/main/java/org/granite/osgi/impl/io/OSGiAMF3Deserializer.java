package org.granite.osgi.impl.io;

import flex.messaging.messages.AbstractMessage;
import org.granite.gravity.Gravity;
import org.granite.messaging.amf.io.AMF3Deserializer;
import org.granite.osgi.impl.OSGiGraniteClassLoader;

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
        Object obj = super.readObject();
        if (obj instanceof AbstractMessage) {
            AbstractMessage message = (AbstractMessage) obj;
            if (Boolean.TRUE.equals(message.getHeader(Gravity.BYTEARRAY_BODY_HEADER))) {
                byte[] byteArray = (byte[]) message.getBody();
                ByteArrayInputStream bais = new ByteArrayInputStream(byteArray);
                AMF3Deserializer deser = new AMF3Deserializer(bais);
                message.setBody(deser.readObject());
            }
            return resolve(obj);
        }
        return obj;
    }


    public Object resolve(Object obj) {
        if (obj instanceof AbstractMessage) {
            AbstractMessage abstractMessage = (AbstractMessage) obj;
            OSGiGraniteClassLoader.setDestination(abstractMessage.getDestination());
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
