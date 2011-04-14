package org.granite.osgi.impl.io;

import org.granite.messaging.amf.io.util.ActionScriptClassDescriptor;
import org.granite.messaging.amf.io.util.MapProperty;

/**
 * @author Franck WOLFF
 */
public class OSGiActionScriptClassDescriptor extends ActionScriptClassDescriptor {

    public OSGiActionScriptClassDescriptor(String type, byte encoding) {
        super(type, encoding);
    }

    @Override
    public void defineProperty(String name) {
        properties.add(new MapProperty(converters, name));
    }

    @Override
    public Object newJavaInstance() {
        return new OSGiDelayedObject(type);
    }
}
