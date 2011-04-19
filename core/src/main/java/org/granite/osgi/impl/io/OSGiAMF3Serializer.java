package org.granite.osgi.impl.io;

import flex.messaging.messages.AbstractMessage;
import org.granite.messaging.amf.io.AMF3Serializer;
import org.granite.osgi.impl.OSGiGraniteClassUtil;

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
    public void writeObject(Object o) throws IOException {
        if (o instanceof AbstractMessage) {
            AbstractMessage abstractMessage = (AbstractMessage) o;
            OSGiGraniteClassUtil.setDestination(abstractMessage.getDestination());
            abstractMessage.setDestination(null);
        }
        super.writeObject(o);
    }
}
