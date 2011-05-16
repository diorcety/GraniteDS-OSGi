package org.granite.osgi.test;


import org.apache.felix.ipojo.ComponentInstance;
import org.apache.log4j.Logger;
import org.granite.config.flex.ServicesConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

import java.io.*;
import java.util.*;

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.LibraryOptions.*;


@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ConfigurationTest {
    private final static Logger logger = Logger.getLogger(ConfigurationTest.class);

    OSGiHelper osgi;
    IPOJOHelper ipojo;

    @Configuration()
    public Option[] config() {
        Option[] opt = CoreOptions.options(
                CoreOptions.felix().version("3.0.8"),
                CoreOptions.systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("WARN"),
                CoreOptions.provision(
                        junitBundles(),

                        CoreOptions.mavenBundle().groupId("org.ops4j.pax.logging").artifactId("pax-logging-api").versionAsInProject().startLevel(1),
                        CoreOptions.mavenBundle().groupId("org.ops4j.pax.logging").artifactId("pax-logging-service").versionAsInProject().startLevel(1),
                        CoreOptions.mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").versionAsInProject().startLevel(2),
                        CoreOptions.mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.configadmin").versionAsInProject().startLevel(2),
                        CoreOptions.mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo.handler.eventadmin").versionAsInProject().startLevel(2),
                        CoreOptions.mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.http.jetty").versionAsInProject().startLevel(2),
                        CoreOptions.mavenBundle().groupId("org.ow2.chameleon.testing").artifactId("osgi-helpers").versionAsInProject().startLevel(2),
                        CoreOptions.mavenBundle().groupId("org.graniteds-osgi").artifactId("granite-core").versionAsInProject().startLevel(3)

                )
        );
        return opt;
    }

    public void setup(BundleContext context) throws IOException {
        osgi = new OSGiHelper(context);
        ipojo = new IPOJOHelper(context);

        // ConfigurationAdmin for logging
        osgi.waitForService(ConfigurationAdmin.class.getName(), null, 60000);

        try {
            Logging.load(context);
        } catch (Exception ex) {
            System.out.println(ex);
        }

        // Needed for all the tests
        osgi.waitForService(ServicesConfig.class.getName(), null, 60000);

        logger.debug("Start!");
    }

    void unsetup() {
        osgi.dispose();
        ipojo.dispose();
    }

    @Test
    public void service(BundleContext context) throws IOException {
        assertThat("Service: Context invalid", context, is(notNullValue()));
        setup(context);

        ServiceReference sr = osgi.getServiceReference("org.granite.config.flex.ServicesConfig");
        ServicesConfig sc = (ServicesConfig) osgi.getServiceObject(sr);
        assertThat("Service: ServicesConfig unavailable", sc, is(notNullValue()));

        ComponentInstance adapter1, adapter2, service1;

        {
            Dictionary properties = new Hashtable();
            properties.put("ID", "service1");

            service1 = ipojo.createComponentInstance("org.granite.config.flex.Service", properties);
        }

        // Correct start
        assertThat("Service: Start failed!", sc.findServiceById("service1"), is(notNullValue()));

        service1.dispose();

        // Correct stop
        assertThat("Service: Stop failed!", sc.findServiceById("service1"), is(nullValue()));

        {
            Dictionary properties = new Hashtable();
            properties.put("ID", "adapter1");
            adapter1 = ipojo.createComponentInstance("org.granite.config.flex.Adapter", properties);
        }
        {
            Dictionary properties = new Hashtable();
            properties.put("ID", "adapter2");
            adapter2 = ipojo.createComponentInstance("org.granite.config.flex.Adapter", properties);
        }
        {
            Dictionary properties = new Hashtable();
            Collection<String> adapters = new LinkedList<String>();
            properties.put("ID", "service1");
            properties.put("MESSAGETYPES", "flex.messaging.messages.AsyncMessage");
            properties.put("DEFAULT_ADAPTER", "adapter1");
            service1 = ipojo.createComponentInstance("org.granite.config.flex.Service", properties);
        }

        // Complex test
        assertThat("Service: Service unavailable", sc.findServiceById("service1"), is(notNullValue()));
        assertTrue("Service: Misconfiguration of messagetypes", sc.findServiceById("service1").getMessageTypes().equals("flex.messaging.messages.AsyncMessage"));
        assertTrue("Service: Invalid default adapter configuration", sc.findServiceById("service1").getDefaultAdapter().getId().equals("adapter1"));

        adapter1.dispose();

        // Test if the service stop it self (adapter dependency)
        assertThat("Service: Invalid service state", sc.findServiceById("service1"), is(nullValue()));

        {
            Dictionary properties = new Hashtable();
            properties.put("ID", "service2");
            properties.put("DEFAULT_ADAPTER", "adapter2");
            service1.dispose();
            service1 = ipojo.createComponentInstance("org.granite.config.flex.Service", properties);
        }

        // Correct behaviour on stop & start
        assertThat("Service: Invalid restart state", sc.findServiceById("service2"), is(notNullValue()));
        assertTrue("Service: Invalid default adapter", sc.findServiceById("service2").getDefaultAdapter().getId().equals("adapter2"));

        service1.dispose();

        adapter2.dispose();

        unsetup();
    }

    @Test
    public void channel(BundleContext context) throws IOException {
        assertThat("Channel: Context invalid", context, is(notNullValue()));

        setup(context);

        ServiceReference sr = osgi.getServiceReference("org.granite.config.flex.ServicesConfig");
        ServicesConfig sc = (ServicesConfig) osgi.getServiceObject(sr);
        assertThat("Channel: ServicesConfig unavailable", sc, is(notNullValue()));

        ComponentInstance channel;
        {
            Dictionary properties = new Hashtable();
            properties.put("ID", "channel1");
            properties.put("ENDPOINT_CLASS", "class1");
            properties.put("ENDPOINT_URI", "/uri1");
            channel = ipojo.createComponentInstance("org.granite.config.flex.Channel", properties);
        }

        assertThat("Channel: Start failed!", sc.findChannelById("channel1"), is(notNullValue()));
        assertTrue("Channel: Misconfiguration of ep uri", sc.findChannelById("channel1").getEndPoint().getUri().equals("/uri1"));
        assertTrue("Channel: Misconfiguration of ep class", sc.findChannelById("channel1").getEndPoint().getClassName().equals("class1"));

        channel.dispose();

        assertThat("Channel: Stop failed!", sc.findChannelById("channel1"), is(nullValue()));

        unsetup();
    }

    @Test
    public void factory(BundleContext context) throws IOException {
        assertThat("Factory: Context invalid", context, is(notNullValue()));

        setup(context);

        ServiceReference sr = osgi.getServiceReference("org.granite.config.flex.ServicesConfig");
        ServicesConfig sc = (ServicesConfig) osgi.getServiceObject(sr);
        assertThat("Factory: ServicesConfig unavailable", sc, is(notNullValue()));

        ComponentInstance factory;
        {
            Dictionary properties = new Hashtable();
            properties.put("ID", "factory1");
            factory = ipojo.createComponentInstance("org.granite.config.flex.Factory", properties);
        }

        assertThat("Factory: Start failed!", sc.findFactoryById("factory1"), is(notNullValue()));

        factory.dispose();

        assertThat("Factory: Stop failed!", sc.findFactoryById("factory1"), is(nullValue()));

        unsetup();
    }

    @Test
    public void destination(BundleContext context) throws IOException {
        assertThat("Destination: Context invalid", context, is(notNullValue()));
        setup(context);

        ServiceReference sr = osgi.getServiceReference("org.granite.config.flex.ServicesConfig");
        ServicesConfig sc = (ServicesConfig) osgi.getServiceObject(sr);
        assertThat("Destination: ServicesConfig unavailable", sc, is(notNullValue()));

        ComponentInstance destination1, channel1, service1, adapter1, adapter2, factory;

        {
            Dictionary properties = new Hashtable();
            properties.put("ID", "adapter1");
            adapter1 = ipojo.createComponentInstance("org.granite.config.flex.Adapter", properties);
        }
        {
            Dictionary properties = new Hashtable();
            properties.put("ID", "adapter2");
            adapter2 = ipojo.createComponentInstance("org.granite.config.flex.Adapter", properties);
        }
        {
            Dictionary properties = new Hashtable();
            properties.put("ID", "service1");
            properties.put("MESSAGETYPES", "MS1");
            service1 = ipojo.createComponentInstance("org.granite.config.flex.Service", properties);
        }
        {
            Dictionary properties = new Hashtable();
            properties.put("ID", "channel1");
            properties.put("CLASS", "org.granite.gravity.channels.GravityChannel");
            properties.put("ENDPOINT_URI", "/uri");
            channel1 = ipojo.createComponentInstance("org.granite.config.flex.Channel", properties);
        }
        {
            Dictionary properties = new Hashtable();
            properties.put("ID", "destination1");
            properties.put("SERVICE", "service1");
            destination1 = ipojo.createComponentInstance("org.granite.config.flex.Destination", properties);
        }

        // Correct start
        assertThat("Destination: Start failed!", sc.findDestinationById("MS1", "destination1"), is(notNullValue()));

        destination1.dispose();

        // Correct stop
        assertThat("Destination: Stop failed!", sc.findDestinationById("MS1", "destination1"), is(nullValue()));

        {
            Dictionary properties = new Hashtable();
            properties.put("ID", "destination1");
            properties.put("SERVICE", "service1");
            destination1 = ipojo.createComponentInstance("org.granite.config.flex.Destination", properties);
        }

        // Invalid find following messagetype
        assertThat("Destination: Invalid configuration messagetypes ignored", sc.findDestinationById("MS2", "service1"), is(nullValue()));

        service1.dispose();

        // Check stop of destination (service dependency)
        assertThat("Destination: Invalid destination state 1", sc.findDestinationById("MS1", "destination1"), is(nullValue()));

        {
            Dictionary properties = new Hashtable();
            properties.put("ID", "service1");
            properties.put("DEFAULT_ADAPTER", "adapter1");
            properties.put("MESSAGETYPES", "MS1");
            service1 = ipojo.createComponentInstance("org.granite.config.flex.Service", properties);
        }

        // Check adapter of destination (service default adapter)
        assertThat("Destination: no adapter 1", sc.findDestinationById("MS1", "destination1").getAdapter(), is(notNullValue()));
        assertTrue("Destination: Invalid adapter 1", sc.findDestinationById("MS1", "destination1").getAdapter().getId().equals("adapter1"));

        {
            Dictionary properties = new Hashtable();
            properties.put("ID", "destination1");
            properties.put("ADAPTER", "adapter2");
            properties.put("SERVICE", "service1");
            destination1.dispose();
            destination1 = ipojo.createComponentInstance("org.granite.config.flex.Destination", properties);
        }

        // Check if destination's adapter overload default service adapter
        assertThat("Destination: no adapter 2", sc.findDestinationById("MS1", "destination1").getAdapter(), is(notNullValue()));
        assertTrue("Destination: Invalid adapter 2", sc.findDestinationById("MS1", "destination1").getAdapter().getId().equals("adapter2"));

        adapter2.dispose();

        // Check stop of destination (adapter dependency)
        assertThat("Destination: Invalid destination state 2", sc.findDestinationById("MS1", "destination1"), is(nullValue()));


        {
            Dictionary properties = new Hashtable();
            properties.put("ID", "destination1");
            properties.put("FACTORY", "factory1");
            properties.put("SERVICE", "service1");
            destination1.dispose();
            destination1 = ipojo.createComponentInstance("org.granite.config.flex.Destination", properties);
        }

        // Check stop of destination (adapter dependency)
        assertThat("Destination: Invalid destination state 3", sc.findDestinationById("MS1", "destination1"), is(nullValue()));

        {
            Dictionary properties = new Hashtable();
            properties.put("ID", "factory1");
            factory = ipojo.createComponentInstance("org.granite.config.flex.Factory", properties);
        }

        // Check stop of destination (adapter dependency)
        assertThat("Destination: Invalid destination state 4", sc.findDestinationById("MS1", "destination1"), is(notNullValue()));

        factory.dispose();
        service1.dispose();
        channel1.dispose();
        adapter1.dispose();

    }

}
