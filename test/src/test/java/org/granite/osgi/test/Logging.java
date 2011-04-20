package org.granite.osgi.test;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Logging {

    static void load(BundleContext context) throws IOException, InvalidSyntaxException {
        ServiceReference caRef = context.getServiceReference(ConfigurationAdmin.class.getName());
        ConfigurationAdmin configAdmin = (ConfigurationAdmin) context.getService(caRef);
        Configuration config = configAdmin.getConfiguration("org.ops4j.pax.logging", null);
        URL url = Logging.class.getClassLoader().getResource("org.ops4j.pax.logging.properties");
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

        Dictionary<String, String> dictionary = new Hashtable<String, String>();

        Pattern pattern = Pattern.compile("^(\\S+)\\s*=\\s*(\\S+)$");
        String line;
        System.out.println("-----------------LOG CONFIG-----------------");
        while ((line = in.readLine()) != null) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                dictionary.put(matcher.group(1), matcher.group(2));
                System.out.println(matcher.group(1) + "=" + matcher.group(2));
            }
        }
        System.out.println("--------------------------------------------");
        config.update(dictionary);
    }
}
