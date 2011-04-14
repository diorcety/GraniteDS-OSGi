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

import org.granite.context.GraniteContext;
import org.granite.logging.Logger;
import org.granite.messaging.amf.AMF3Constants;
import org.granite.messaging.amf.io.AMF3Deserializer;
import org.granite.messaging.amf.io.AMF3SerializationException;
import org.granite.messaging.amf.io.util.ActionScriptClassDescriptor;
import org.granite.messaging.amf.io.util.DefaultActionScriptClassDescriptor;
import org.granite.messaging.amf.io.util.externalizer.Externalizer;
import org.granite.messaging.amf.io.util.instantiator.AbstractInstantiator;
import org.granite.util.ClassUtil;
import org.granite.util.XMLUtil;
import org.w3c.dom.Document;

import java.io.*;
import java.util.*;

/**
 * @author Franck WOLFF
 */
public class OSGiAMF3Deserializer extends DataInputStream implements ObjectInput, AMF3Constants {

    ///////////////////////////////////////////////////////////////////////////
    // Fields.

    protected static final Logger log = Logger.getLogger(AMF3Deserializer.class);
    protected static final Logger logMore = Logger.getLogger(AMF3Deserializer.class.getName() + "_MORE");

    protected final List<String> storedStrings = new ArrayList<String>();
    protected final List<Object> storedObjects = new ArrayList<Object>();
    protected final List<ActionScriptClassDescriptor> storedClassDescriptors = new ArrayList<ActionScriptClassDescriptor>();

    protected final GraniteContext context = GraniteContext.getCurrentInstance();

    protected final XMLUtil xmlUtil = new XMLUtil();

    protected final boolean debug;
    protected final boolean debugMore;

    ///////////////////////////////////////////////////////////////////////////
    // Constructor.

    public OSGiAMF3Deserializer(InputStream in) {
        super(in);

        debug = log.isDebugEnabled();
        debugMore = logMore.isDebugEnabled();

        if (debugMore) logMore.debug("new AMF3Deserializer(in=%s)", in);
    }

    ///////////////////////////////////////////////////////////////////////////
    // ObjectInput implementation.

    public Object readObject() throws IOException {
        if (debugMore) logMore.debug("readObject()...");

        try {
	        int type = readAMF3Integer();
	        return readObject(type);
        }
        catch (IOException e) {
        	throw e;
        }
        catch (Exception e) {
        	throw new AMF3SerializationException(e);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // AMF3 deserialization.

    protected Object readObject(int type) throws IOException {

        if (debugMore) logMore.debug("readObject(type=0x%02X)", type);

        switch (type) {
        case AMF3_UNDEFINED: // 0x00;
        case AMF3_NULL: // 0x01;
            return null;
        case AMF3_BOOLEAN_FALSE: // 0x02;
            return Boolean.FALSE;
        case AMF3_BOOLEAN_TRUE: // 0x03;
            return Boolean.TRUE;
        case AMF3_INTEGER: // 0x04;
            return Integer.valueOf(readAMF3Integer());
        case AMF3_NUMBER: // 0x05;
            return readAMF3Double();
        case AMF3_STRING: // 0x06;
            return readAMF3String();
        case AMF3_XML: // 0x07;
            return readAMF3Xml();
        case AMF3_DATE: // 0x08;
            return readAMF3Date();
        case AMF3_ARRAY: // 0x09;
            return readAMF3Array();
        case AMF3_OBJECT: // 0x0A;
            return readAMF3Object();
        case AMF3_XMLSTRING: // 0x0B;
            return readAMF3XmlString();
        case AMF3_BYTEARRAY: // 0x0C;
            return readAMF3ByteArray();

        case AMF3_VECTOR_INT: // 0x0D;
        	return readAMF3VectorInt();
        case AMF3_VECTOR_UINT: // 0x0E;
        	return readAMF3VectorUInt();
        case AMF3_VECTOR_NUMBER: // 0x0F;
        	return readAMF3VectorNumber();
        case AMF3_VECTOR_OBJECT: // 0x10;
        	return readAMF3VectorObject();

        default:
            throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    protected int readAMF3Integer() throws IOException {
        int result = 0;

        int n = 0;
        int b = readUnsignedByte();
        while ((b & 0x80) != 0 && n < 3) {
            result <<= 7;
            result |= (b & 0x7f);
            b = readUnsignedByte();
            n++;
        }
        if (n < 3) {
            result <<= 7;
            result |= b;
        } else {
            result <<= 8;
            result |= b;
            if ((result & 0x10000000) != 0)
                result |= 0xe0000000;
        }

        if (debugMore) logMore.debug("readAMF3Integer() -> %d", result);

        return result;
    }

    protected Double readAMF3Double() throws IOException {
        double d = readDouble();
        Double result = (Double.isNaN(d) ? null : Double.valueOf(d));

        if (debugMore) logMore.debug("readAMF3Double() -> %f", result);

        return result;
    }

    protected String readAMF3String() throws IOException {
        String result = null;

        if (debugMore) logMore.debug("readAMF3String()...");

        int type = readAMF3Integer();
        if ((type & 0x01) == 0) // stored string
            result = getFromStoredStrings(type >> 1);
        else {
            int length = type >> 1;
            if (debugMore) logMore.debug("readAMF3String() - length=%d", length);

            if (length > 0) {

                byte[] utfBytes = new byte[length];
                char[] utfChars = new char[length];

                readFully(utfBytes);

                int c, c2, c3, iBytes = 0, iChars = 0;
                while (iBytes < length) {
                    c = utfBytes[iBytes++] & 0xFF;
                    if (c <= 0x7F)
                        utfChars[iChars++] = (char)c;
                    else {
                        switch (c >> 4) {
                        case 12: case 13:
                            c2 = utfBytes[iBytes++];
                            if ((c2 & 0xC0) != 0x80)
                                throw new UTFDataFormatException("Malformed input around byte " + (iBytes-2));
                            utfChars[iChars++] = (char)(((c & 0x1F) << 6) | (c2 & 0x3F));
                            break;
                        case 14:
                            c2 = utfBytes[iBytes++];
                            c3 = utfBytes[iBytes++];
                            if (((c2 & 0xC0) != 0x80) || ((c3 & 0xC0) != 0x80))
                                throw new UTFDataFormatException("Malformed input around byte " + (iBytes-3));
                            utfChars[iChars++] = (char)(((c & 0x0F) << 12) | ((c2 & 0x3F) << 6) | ((c3 & 0x3F) << 0));
                            break;
                        default:
                            throw new UTFDataFormatException("Malformed input around byte " + (iBytes-1));
                        }
                    }
                }
                result = new String(utfChars, 0, iChars);

                if (debugMore) logMore.debug("readAMF3String() - result=%s", result);

                addToStoredStrings(result);
            } else
                result = "";
        }

        if (debugMore) logMore.debug("readAMF3String() -> %s", result);

        return result;
    }


    protected Date readAMF3Date() throws IOException {
        Date result = null;

        int type = readAMF3Integer();
        if ((type & 0x01) == 0) // stored Date
            result = (Date)getFromStoredObjects(type >> 1);
        else {
            result = new Date((long)readDouble());
            addToStoredObjects(result);
        }

        if (debugMore) logMore.debug("readAMF3Date() -> %s", result);

        return result;
    }

    protected Object readAMF3Array() throws IOException {
        Object result = null;

        int type = readAMF3Integer();
        if ((type & 0x01) == 0) // stored array.
            result = getFromStoredObjects(type >> 1);
        else {
            final int size = type >> 1;

            String key = readAMF3String();
            if (key.length() == 0) {
                Object[] objects = new Object[size];
                addToStoredObjects(objects);

                for (int i = 0; i < size; i++)
                    objects[i] = readObject();

                result = objects;
            }
            else {
                Map<Object, Object> map = new HashMap<Object, Object>();
                addToStoredObjects(map);

                while(key.length() > 0) {
                    map.put(key, readObject());
                    key = readAMF3String();
                }
                for (int i = 0; i < size; i++)
                    map.put(Integer.valueOf(i), readObject());

                result = map;
            }
        }

        if (debugMore) logMore.debug("readAMF3Array() -> %s", result);

        return result;
    }

    protected Object readAMF3VectorInt() throws IOException {
        Object result = null;

        int type = readAMF3Integer();
        if ((type & 0x01) == 0) // stored vector.
        	result = getFromStoredObjects(type >> 1);
        else {
        	final int length = type >> 1;
            List<Integer> vector = new ArrayList<Integer>(length);
            
            addToStoredObjects(result);
            
            readAMF3Integer(); // always 0x00?
            
            for (int i = 0; i < length; i++)
            	vector.add(readInt());
            
            result = vector;
        }
        
        if (debugMore) logMore.debug("readAMF3VectorInt() -> %s", result);

        return result;
    }

    protected Object readAMF3VectorUInt() throws IOException {
        Object result = null;

        int type = readAMF3Integer();
        if ((type & 0x01) == 0) // stored vector.
        	result = getFromStoredObjects(type >> 1);
        else {
        	final int length = type >> 1;
            List<Long> vector = new ArrayList<Long>(length);
            
            addToStoredObjects(result);
            
            readAMF3Integer(); // always 0x00?
            
            for (int i = 0; i < length; i++)
            	vector.add(readInt() & 0xffffffffL);
            
            result = vector;
        }
        
        if (debugMore) logMore.debug("readAMF3VectorUInt() -> %s", result);

        return result;
    }

    protected Object readAMF3VectorNumber() throws IOException {
        Object result = null;

        int type = readAMF3Integer();
        if ((type & 0x01) == 0) // stored vector.
        	result = getFromStoredObjects(type >> 1);
        else {
        	final int length = type >> 1;
            List<Double> vector = new ArrayList<Double>(length);
            
            addToStoredObjects(result);
            
            readAMF3Integer(); // always 0x00?
            
            for (int i = 0; i < length; i++)
            	vector.add(readDouble());
            
            result = vector;
        }
        
        if (debugMore) logMore.debug("readAMF3VectorDouble() -> %s", result);

        return result;
    }

    protected Object readAMF3VectorObject() throws IOException {
        Object result = null;

        int type = readAMF3Integer();
        if ((type & 0x01) == 0) // stored vector.
        	result = getFromStoredObjects(type >> 1);
        else {
        	final int length = type >> 1;
            List<Object> vector = new ArrayList<Object>(length);
            
            addToStoredObjects(result);
            
            readAMF3Integer(); // always 0x00?
            readAMF3Integer(); // always 0x01?
            
            for (int i = 0; i < length; i++)
            	vector.add(readObject());
            
            result = vector;
        }
        
        if (debugMore) logMore.debug("readAMF3VectorObject() -> %s", result);

        return result;
    }

    protected Object readAMF3Object() throws IOException {
        if (debug) log.debug("readAMF3Object()...");

        Object result = null;

        int type = readAMF3Integer();
        if (debug) log.debug("readAMF3Object() - type=0x%02X", type);

        if ((type & 0x01) == 0) // stored object.
            result = getFromStoredObjects(type >> 1);
        else {
            boolean inlineClassDef = (((type >> 1) & 0x01) != 0);
            if (debug) log.debug("readAMF3Object() - inlineClassDef=%b", inlineClassDef);

            // read class decriptor.
            ActionScriptClassDescriptor desc = null;
            if (inlineClassDef) {
                int propertiesCount = type >> 4;
                if (debug) log.debug("readAMF3Object() - propertiesCount=%d", propertiesCount);

                byte encoding = (byte)((type >> 2) & 0x03);
                if (debug) log.debug("readAMF3Object() - encoding=%d", encoding);

                String className = readAMF3String();
                if (debug) log.debug("readAMF3Object() - className=%s", className);

                // try to find out custom AS3 class descriptor
                Class<? extends ActionScriptClassDescriptor> descriptorType = null;

                try {
                    if (!"".equals(className))
                        descriptorType = context.getGraniteConfig().getActionScriptDescriptor(className);
                    if (debug) log.debug("readAMF3Object() - descriptorType=%s", descriptorType);

                    if (descriptorType != null) {
                        // instantiate descriptor
                        Class<?>[] argsDef = new Class[]{String.class, byte.class};
                        Object[] argsVal = new Object[]{className, Byte.valueOf(encoding)};
                        try {
                            desc = ClassUtil.newInstance(descriptorType, argsDef, argsVal);
                        } catch (Exception e) {
                            throw new RuntimeException("Could not instantiate AS descriptor: " + descriptorType, e);
                        }
                    }

                    if (desc == null)
                        desc = new DefaultActionScriptClassDescriptor(className, encoding);

                } catch (Exception e) {
                    desc = new OSGiActionScriptClassDescriptor(className, encoding);
                }
                addToStoredClassDescriptors(desc);

                if (debug) log.debug("readAMF3Object() - defining %d properties...", propertiesCount);
                for (int i = 0; i < propertiesCount; i++) {
                    String name = readAMF3String();
                    if (debug) log.debug("readAMF3Object() - defining property name=%s", name);
                    desc.defineProperty(name);
                }
            } else
                desc = getFromStoredClassDescriptors(type >> 2);

            if (debug) log.debug("readAMF3Object() - actionScriptClassDescriptor=%s", desc);

            int objectEncoding = desc.getEncoding();

            // Find externalizer and create Java instance.
            Externalizer externalizer = desc.getExternalizer();
            if (externalizer != null) {
                try {
                    result = externalizer.newInstance(desc.getType(), this);
                } catch (Exception e) {
                    throw new RuntimeException("Could not instantiate type: " + desc.getType(), e);
                }
            } else
                result = desc.newJavaInstance();

            int index = addToStoredObjects(result);
            
            // Entity externalizers (eg. OpenJPA) may return null values for non-null AS3 objects (ie. proxies).
            if (result == null) {
            	if (debug) log.debug("readAMF3Object() - Added null object to stored objects for actionScriptClassDescriptor=%s", desc);
            	return null;
            }

            // read object content...
            if ((objectEncoding & 0x01) != 0) {
                // externalizer.
                if (externalizer != null) {
                    if (debug) log.debug("readAMF3Object() - using externalizer=%s", externalizer);
                    try {
                        externalizer.readExternal(result, this);
                    } catch (IOException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException("Could not read externalized object: " + result, e);
                    }
                }
                // legacy externalizable.
                else {
                	if (debug) log.debug("readAMF3Object() - legacy Externalizable=%s", result.getClass());
                	if (!(result instanceof Externalizable)) {
                		throw new RuntimeException(
                			"The ActionScript3 class bound to " + result.getClass().getName() +
                			" (ie: [RemoteClass(alias=\"" + result.getClass().getName() + "\")])" +
                			" implements flash.utils.IExternalizable but this Java class neither" +
                			" implements java.io.Externalizable nor is in the scope of a configured" +
                			" externalizer (please fix your granite-config.xml)"
                		);
                	}
                    try {
                        ((Externalizable)result).readExternal(this);
                    } catch (IOException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException("Could not read externalizable object: " + result, e);
                    }
                }
            }
            else {
                // defined values...
                if (desc.getPropertiesCount() > 0) {
                    if (debug) log.debug("readAMF3Object() - reading defined properties...");
                    for (int i = 0; i < desc.getPropertiesCount(); i++) {
                        byte vType = readByte();
                        Object value = readObject(vType);
                        if (debug) log.debug("readAMF3Object() - setting defined property: %s=%s", desc.getPropertyName(i), value);
                        desc.setPropertyValue(i, result, value);
                    }
                }

                // dynamic values...
                if (objectEncoding == 0x02) {
                    if (debug) log.debug("readAMF3Object() - reading dynamic properties...");
                    while (true) {
                        String name = readAMF3String();
                        if (name.length() == 0)
                            break;
                        byte vType = readByte();
                        Object value = readObject(vType);
                        if (debug) log.debug("readAMF3Object() - setting dynamic property: %s=%s", name, value);
                        desc.setPropertyValue(name, result, value);
                    }
                }
            }


            if (result instanceof AbstractInstantiator<?>) {
                if (debug) log.debug("readAMF3Object() - resolving instantiator...");
                try {
                    result = ((AbstractInstantiator<?>)result).resolve();
                } catch (Exception e) {
                    throw new RuntimeException("Could not instantiate object: " + result, e);
                }
                setStoredObject(index, result);
            }

        }

        if (debug) log.debug("readAMF3Object() -> %s", result);

        return result;
    }

    protected Document readAMF3Xml() throws IOException {
        String xml = readAMF3XmlString();
        Document result = xmlUtil.buildDocument(xml);

        if (debugMore) logMore.debug("readAMF3Xml() -> %s", result);

        return result;
    }

    protected String readAMF3XmlString() throws IOException {
        String result = null;

        int type = readAMF3Integer();
        if ((type & 0x01) == 0) // stored String
            result = getFromStoredStrings(type >> 1);
        else {
            byte[] bytes = readBytes(type >> 1);
            result = new String(bytes, "UTF-8");
            addToStoredStrings(result);
        }

        if (debugMore) logMore.debug("readAMF3XmlString() -> %s", result);

        return result;
    }

    protected byte[] readAMF3ByteArray() throws IOException {
        byte[] result = null;

        int type = readAMF3Integer();
        if ((type & 0x01) == 0) // stored object.
            result = (byte[])getFromStoredObjects(type >> 1);
        else {
            result = readBytes(type >> 1);
            addToStoredObjects(result);
        }

        if (debugMore) logMore.debug("readAMF3ByteArray() -> %s", result);

        return result;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Cached objects methods.

    protected void addToStoredStrings(String s) {
        if (debug) log.debug("addToStoredStrings(s=%s) at index=%d", s, storedStrings.size());
        storedStrings.add(s);
    }

    protected String getFromStoredStrings(int index) {
        if (debug) log.debug("getFromStoredStrings(index=%d)", index);
        String s = storedStrings.get(index);
        if (debug) log.debug("getFromStoredStrings() -> %s", s);
        return s;
    }

    protected int addToStoredObjects(Object o) {
        int index = storedObjects.size();
        if (debug) log.debug("addToStoredObjects(o=%s) at index=%d", o, index);
        storedObjects.add(o);
        return index;
    }

    protected void setStoredObject(int index, Object o) {
        if (debug) log.debug("setStoredObject(index=%d, o=%s)", index, o);
        storedObjects.set(index, o);
    }

    protected Object getFromStoredObjects(int index) {
        if (debug) log.debug("getFromStoredObjects(index=%d)", index);
        Object o = storedObjects.get(index);
        if (debug) log.debug("getFromStoredObjects() -> %s", o);
        return o;
    }

    protected void addToStoredClassDescriptors(ActionScriptClassDescriptor desc) {
        if (debug) log.debug("addToStoredClassDescriptors(desc=%s) at index=%d", desc, storedClassDescriptors.size());
        storedClassDescriptors.add(desc);
    }

    protected ActionScriptClassDescriptor getFromStoredClassDescriptors(int index) {
        if (debug) log.debug("getFromStoredClassDescriptors(index=%d)", index);
        ActionScriptClassDescriptor desc = storedClassDescriptors.get(index);
        if (debug) log.debug("getFromStoredClassDescriptors() -> %s", desc);
        return desc;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Utilities.

    protected byte[] readBytes(int count) throws IOException {
        byte[] bytes = new byte[count];
        readFully(bytes);
        return bytes;
    }
}
