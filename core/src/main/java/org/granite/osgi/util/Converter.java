package org.granite.osgi.util;

import org.granite.util.XMap;

import java.util.Dictionary;
import java.util.Enumeration;

public class Converter {
    static public XMap getXMap(Dictionary<String, String> dictionary) {
        XMap properties = new XMap();
        if (dictionary != null) {
            Enumeration enumeration = dictionary.keys();
            while (enumeration.hasMoreElements()) {
                String id = (String) enumeration.nextElement();
                properties.put(id, (String) dictionary.get(id));
            }
        }
        return properties;
    }
}
