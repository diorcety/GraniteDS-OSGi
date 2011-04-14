package org.granite.osgi.impl.io;

import flex.messaging.messages.CommandMessage;
import flex.messaging.messages.RemotingMessage;
import org.granite.context.IGraniteContext;
import org.granite.messaging.amf.AMF0Body;
import org.granite.messaging.amf.AMF0Message;

import java.util.Iterator;
import java.util.List;

public class OSGiResolver {
    private final IGraniteContext graniteContext;

    public OSGiResolver(IGraniteContext graniteContext) {
        this.graniteContext = graniteContext;
    }

    public Object resolve(Object obj) {
        if (obj instanceof AMF0Message) {
            AMF0Message message = (AMF0Message) obj;
            for (Iterator<AMF0Body> ibody = message.getBodies(); ibody.hasNext();) {
                AMF0Body body = ibody.next();
                body.setValue(resolve(body.getValue()));
            }

        } else if (obj instanceof RemotingMessage) {
            RemotingMessage remotingMessage = (RemotingMessage) obj;
            graniteContext.getRequestMap().put("destination", remotingMessage.getDestination());
            remotingMessage.setBody(resolve(remotingMessage.getBody()));

        } else if (obj instanceof CommandMessage) {
            CommandMessage commandMessage = (CommandMessage) obj;
            graniteContext.getRequestMap().put("destination", commandMessage.getDestination());
            commandMessage.setBody(resolve(commandMessage.getBody()));

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
