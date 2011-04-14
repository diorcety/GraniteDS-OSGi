/*
 * www.openamf.org
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.granite.osgi.impl.io;

import flex.messaging.io.ASObject;
import flex.messaging.messages.CommandMessage;
import flex.messaging.messages.RemotingMessage;
import org.granite.context.IGraniteContext;
import org.granite.logging.Logger;
import org.granite.messaging.amf.AMF0Body;
import org.granite.messaging.amf.AMF0Header;
import org.granite.messaging.amf.AMF0Message;
import org.granite.messaging.amf.io.AMF0Deserializer;
import org.granite.messaging.amf.io.util.instantiator.AbstractInstantiator;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.*;

/**
 * AMF Deserializer
 *
 * @author Jason Calabrese <jasonc@missionvi.com>
 * @author Pat Maddox <pergesu@users.sourceforge.net>
 * @author Sylwester Lachiewicz <lachiewicz@plusnet.pl>
 * @version $Revision: 1.38 $, $Date: 2004/12/09 04:50:07 $
 */
public class OSGiAMF0Deserializer {

    private static final Logger log = Logger.getLogger(AMF0Deserializer.class);

    private List<Object> storedObjects = null;

    /**
     * The AMF input stream
     */
    private final InputStream rawInputStream;
    private final DataInputStream dataInputStream;

    /**
     * Object to store the deserialized data
     */
    private final AMF0Message message = new AMF0Message();
    private final OSGiAMF3Deserializer amf3;

    /**
     * Deserialize message
     *
     * @param inputStream message input stream
     * @throws java.io.IOException
     */
    public OSGiAMF0Deserializer(InputStream inputStream) throws IOException {
        //log.info("Deserializing Message, for more info turn on debug level");

        // Save the input stream for this object
    	this.rawInputStream = inputStream;
        this.dataInputStream = inputStream instanceof DataInputStream
        	? ((DataInputStream)inputStream)
        	: new DataInputStream(inputStream);

        this.amf3 = new OSGiAMF3Deserializer(rawInputStream);

        // Read the binary header
        readHeaders();
        log.debug("readHeader");
        // Read the binary body
        readBodies();
        log.debug("readBody");
    }

    public AMF0Message getAMFMessage() {
        return message;
    }

    /**
     * Read message header
     *
     * @throws java.io.IOException
     */
    protected void readHeaders() throws IOException {
        // version
        message.setVersion(dataInputStream.readUnsignedShort());
        // Find total number of header elements
        int headerCount = dataInputStream.readUnsignedShort();
        log.debug("headerCount = %d", headerCount);

        // Loop over all the header elements
        for (int i = 0; i < headerCount; i++) {
            // clear storedObjects - references are new for every header
            storedObjects = new ArrayList<Object>();
            String key = dataInputStream.readUTF();
            // Find the must understand flag
            boolean required = dataInputStream.readBoolean();
            // Grab the length of the header element
            /*long length =*/ dataInputStream.readInt();
            // Grab the type of the element
            byte type = dataInputStream.readByte();
            // Turn the element into real data
            Object value = readData(type);
            // Save the name/value into the headers array
            message.addHeader(key, required, value);
        }
    }

    /**
     * Read message body
     *
     * @throws java.io.IOException
     */
    protected void readBodies() throws IOException {
        // Find the total number of body elements
        int bodyCount = dataInputStream.readUnsignedShort();
        log.debug("bodyCount = %d", bodyCount);

        // Loop over all the body elements
        for (int i = 0; i < bodyCount; i++) {
            //clear storedObjects
            storedObjects = new ArrayList<Object>();
            // The target method
            String method = dataInputStream.readUTF();
            // The target that the client understands
            String target = dataInputStream.readUTF();
            // Get the length of the body element
            /*long length =*/ dataInputStream.readInt();
            // Grab the type of the element
            byte type = dataInputStream.readByte();
            log.debug("type = 0x%02X", type);
            // Turn the argument elements into real data
            Object data = readData(type);
            // Add the body element to the body object
            message.addBody(method, target, data, type);
        }
    }

    /**
     * Reads custom class
     *
     * @return the read Object
     * @throws java.io.IOException
     */
    protected Object readCustomClass() throws IOException {
        // Grab the explicit type - somehow it works
        String type = dataInputStream.readUTF();
        log.debug("Reading Custom Class: %s", type);
        /*
        String mappedJavaClass = OpenAMFConfig.getInstance().getJavaClassName(type);
        if (mappedJavaClass != null) {
            type = mappedJavaClass;
        }
        */
        ASObject aso = new ASObject(type);
        // The rest of the bytes are an object without the 0x03 header
        return readObject(aso);
    }

    protected ASObject readObject() throws IOException {
        ASObject aso = new ASObject();
        return readObject(aso);
    }

    /**
     * Reads an object and converts the binary data into an List
     *
     * @param aso
     * @return the read object
     * @throws java.io.IOException
     */
    protected ASObject readObject(ASObject aso) throws IOException {
        storeObject(aso);
        // Init the array
        log.debug("reading object");
        // Grab the key
        String key = dataInputStream.readUTF();
        for (byte type = dataInputStream.readByte();
             type != 9;
             type = dataInputStream.readByte()) {
            // Grab the value
            Object value = readData(type);
            // Save the name/value pair in the map
            if (value == null) {
                log.info("Skipping NULL value for : %s", key);
            } else {
                aso.put(key, value);
                log.debug(" adding {key=%s, value=%s, type=%d}", key, value, type);
            }
            // Get the next name
            key = dataInputStream.readUTF();
        }
        log.debug("finished reading object");
        // Return the map
        return aso;
    }

    /**
     * Reads array
     *
     * @return the read array (as a list).
     * @throws java.io.IOException
     */
    protected List<?> readArray() throws IOException {
        // Init the array
        List<Object> array = new ArrayList<Object>();
        storeObject(array);
        log.debug("Reading array");
        // Grab the length of the array
        long length = dataInputStream.readInt();
        log.debug("array length = %d", length);
        // Loop over all the elements in the data
        for (long i = 0; i < length; i++) {
            // Grab the type for each element
            byte type = dataInputStream.readByte();
            // Grab the element
            Object data = readData(type);
            array.add(data);
        }
        // Return the data
        return array;
    }

    /**
     * Store object in  internal array
     *
     * @param o the object to store
     */
    private void storeObject(Object o) {
        storedObjects.add(o);
        log.debug("storedObjects.size: %d", storedObjects.size());
    }

    /**
     * Reads date
     *
     * @return the read date
     * @throws java.io.IOException
     */
    protected Date readDate() throws IOException {
        long ms = (long) dataInputStream.readDouble(); // Date in millis from 01/01/1970

      // here we have to read in the raw
      // timezone offset (which comes in minutes, but incorrectly signed),
      // make it millis, and fix the sign.
      int timeoffset = dataInputStream.readShort() * 60000 * -1; // now we have millis

      TimeZone serverTimeZone = TimeZone.getDefault();

      // now we subtract the current timezone offset and add the one that was passed
      // in (which is of the Flash client), which gives us the appropriate ms (i think)
      // -alon
      Calendar sent = new GregorianCalendar();
      sent.setTime( (new Date(ms - serverTimeZone.getRawOffset() + timeoffset)));

      TimeZone sentTimeZone = sent.getTimeZone();

      // we have to handle daylight savings ms as well
      if (sentTimeZone.inDaylightTime(sent.getTime()))
      {
          //
          // Implementation note: we are trying to maintain compatibility
          // with J2SE 1.3.1
          //
          // As such, we can't use java.util.Calendar.getDSTSavings() here
          //
        sent.setTime(new Date(sent.getTime().getTime() - 3600000));
      }

      return sent.getTime();
    }

    /**
     * Reads flushed stored object
     *
     * @return the stored object
     * @throws java.io.IOException
     */
    protected Object readFlushedSO() throws IOException {
        int index = dataInputStream.readUnsignedShort();
        log.debug("Object Index: %d", index);
        return storedObjects.get(index);
    }

    /**
     * Reads object
     *
     * @return always null...
     */
    protected Object readASObject() {
        return null;
    }

    /**
     * Reads object
     *
     * @return the AMF3 decoded object
     */
    protected Object readAMF3Data() throws IOException {
        try {
            return amf3.readObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads object from inputstream with selected type
     *
     * @param type
     * @return the read object
     * @throws java.io.IOException
     */
    protected Object readData(byte type) throws IOException {
        log.debug("Reading data of type: %s", AMF0Body.getObjectTypeDescription(type));
        switch (type) {
            case AMF0Body.DATA_TYPE_NUMBER: // 0
                return new Double(dataInputStream.readDouble());
            case AMF0Body.DATA_TYPE_BOOLEAN: // 1
                return new Boolean(dataInputStream.readBoolean());
            case AMF0Body.DATA_TYPE_STRING: // 2
                return dataInputStream.readUTF();
            case AMF0Body.DATA_TYPE_OBJECT: // 3
                return readObject();
            case AMF0Body.DATA_TYPE_MOVIE_CLIP: // 4
                throw new IOException("Unknown/unsupported object type " + AMF0Body.getObjectTypeDescription(type));
            case AMF0Body.DATA_TYPE_NULL: // 5
            case AMF0Body.DATA_TYPE_UNDEFINED: //6
                return null;
            case AMF0Body.DATA_TYPE_REFERENCE_OBJECT: // 7
                return readFlushedSO();
            case AMF0Body.DATA_TYPE_MIXED_ARRAY: // 8
                /*long length =*/ dataInputStream.readInt();
                //don't do anything with the length
                return readObject();
            case AMF0Body.DATA_TYPE_OBJECT_END: // 9
                return null;
            case AMF0Body.DATA_TYPE_ARRAY: // 10
                return readArray();
            case AMF0Body.DATA_TYPE_DATE: // 11
                return readDate();
            case AMF0Body.DATA_TYPE_LONG_STRING: // 12
                return readLongUTF(dataInputStream);
            case AMF0Body.DATA_TYPE_AS_OBJECT: // 13
                return readASObject();
            case AMF0Body.DATA_TYPE_RECORDSET: // 14
                return null;
            case AMF0Body.DATA_TYPE_XML: // 15
                return convertToDOM(dataInputStream);
            case AMF0Body.DATA_TYPE_CUSTOM_CLASS: // 16
                return readCustomClass();
            case AMF0Body.DATA_TYPE_AMF3_OBJECT: // 17
                return readAMF3Data();
            default :
                throw new IOException("Unknown/unsupported object type " + AMF0Body.getObjectTypeDescription(type));
        }
    }

    /**
     * This is a hacked verison of Java's DataInputStream.readUTF(), which only
     * supports Strings <= 65535 UTF-8-encoded characters
     */
    private Object readLongUTF(DataInputStream in) throws IOException {
        int utflen = in.readInt();
        StringBuffer str = new StringBuffer(utflen);
        byte bytearr [] = new byte[utflen];
        int c, char2, char3;
        int count = 0;

        in.readFully(bytearr, 0, utflen);

        while (count < utflen) {
            c = bytearr[count] & 0xff;
            switch (c >> 4) {
                case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
                    /* 0xxxxxxx*/
                    count++;
                    str.append((char)c);
                    break;
                case 12: case 13:
                    /* 110x xxxx   10xx xxxx*/
                    count += 2;
                    if (count > utflen)
                        throw new UTFDataFormatException();
                    char2 = bytearr[count-1];
                    if ((char2 & 0xC0) != 0x80)
                        throw new UTFDataFormatException();
                    str.append((char)(((c & 0x1F) << 6) | (char2 & 0x3F)));
                    break;
                case 14:
                    /* 1110 xxxx  10xx xxxx  10xx xxxx */
                    count += 3;
                    if (count > utflen)
                        throw new UTFDataFormatException();
                    char2 = bytearr[count-2];
                    char3 = bytearr[count-1];
                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
                        throw new UTFDataFormatException();
                    str.append((char)(((c     & 0x0F) << 12) |
                                      ((char2 & 0x3F) << 6)  |
                                      ((char3 & 0x3F) << 0)));
                    break;
                default:
                    /* 10xx xxxx,  1111 xxxx */
                    throw new UTFDataFormatException();
            }
        }

        // The number of chars produced may be less than utflen
        return new String(str);
    }

    public static Document convertToDOM(DataInputStream is) throws IOException {
        Document document = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        int length = is.readInt();
        try {
            byte[] buf = new byte[length];
            is.readFully(buf, 0, length);
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(new ByteArrayInputStream(buf));
        } catch (Exception e) {
            throw new IOException("Error while parsing xml: " + e.getMessage());
        }
        return document;
    }
}