package org.granite.osgi.test;


import org.apache.felix.ipojo.ComponentInstance;
import org.apache.log4j.Logger;
import org.granite.config.flex.Service;
import org.granite.config.flex.ServicesConfig;
import org.granite.osgi.ConfigurationHelper;
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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.LibraryOptions.*;


@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ConfigurationTest {
    private final static Logger logger = Logger.getLogger(ConfigurationTest.class);

    OSGiHelper osgi;

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

        // ConfigurationAdmin for logging
        osgi.waitForService(ConfigurationAdmin.class.getName(), null, 60000);

        try {
            Logging.load(context);
        } catch (Exception ex) {
            System.out.println(ex);
        }

        // Needed for all the tests
        osgi.waitForService(ServicesConfig.class.getName(), null, 60000);
        osgi.waitForService(ConfigurationHelper.class.getName(), null, 60000);
        logger.debug("Start!");
    }

    void unsetup() {
        osgi.dispose();
    }

    @Test
    public void service(BundleContext context) throws IOException {
        assertThat("Service: Context invalid", context, is(notNullValue()));
        setup(context);

        ServiceReference sr = osgi.getServiceReference("org.granite.config.flex.ServicesConfig");
        ServicesConfig sc = (ServicesConfig) osgi.getServiceObject(sr);
        ServiceReference sr2 = osgi.getServiceReference("org.granite.osgi.ConfigurationHelper");
        ConfigurationHelper ch = (ConfigurationHelper) osgi.getServiceObject(sr2);
        assertThat("Service: ServicesConfig unavailable", sc, is(notNullValue()));

        ComponentInstance adapter1, adapter2, service1;


        service1 = ch.newGraniteService("service1");


        // Correct start
        assertThat("Service: Start failed!", sc.findServiceById("service1"), is(notNullValue()));

        service1.dispose();

        // Correct stop
        assertThat("Service: Stop failed!", sc.findServiceById("service1"), is(nullValue()));

        adapter1 = ch.newAdapter("adapter1");
        adapter2 = ch.newAdapter("adapter2");
        service1 = ch.newGravityService("service1", "adapter1");

        // Complex test
        assertThat("Service: Service unavailable", sc.findServiceById("service1"), is(notNullValue()));
        assertTrue("Service: Misconfiguration of messagetypes", sc.findServiceById("service1").getMessageTypes().equals("flex.messaging.messages.AsyncMessage"));
        assertTrue("Service: Invalid default adapter configuration", sc.findServiceById("service1").getDefaultAdapter().getId().equals("adapter1"));

        adapter1.dispose();

        // Test if the service stop it self (adapter dependency)
        assertThat("Service: Invalid service state", sc.findServiceById("service1"), is(nullValue()));

        service1.dispose();
        service1 = ch.newGravityService("service2", "adapter2");

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
        ServiceReference sr2 = osgi.getServiceReference("org.granite.osgi.ConfigurationHelper");
        ConfigurationHelper ch = (ConfigurationHelper) osgi.getServiceObject(sr2);
        assertThat("Channel: ServicesConfig unavailable", sc, is(notNullValue()));

        ComponentInstance channel;
        channel = ch.newGraniteChannel("channel1", "/uri1");

        assertThat("Channel: Start failed!", sc.findChannelById("channel1"), is(notNullValue()));
        assertTrue("Channel: Misconfiguration of ep uri", sc.findChannelById("channel1").getEndPoint().getUri().equals("/uri1"));

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
        ServiceReference sr2 = osgi.getServiceReference("org.granite.osgi.ConfigurationHelper");
        ConfigurationHelper ch = (ConfigurationHelper) osgi.getServiceObject(sr2);
        assertThat("Factory: ServicesConfig unavailable", sc, is(notNullValue()));

        ComponentInstance factory;
        factory = ch.newFactory("factory1");


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
        ServiceReference sr2 = osgi.getServiceReference("org.granite.osgi.ConfigurationHelper");
        ConfigurationHelper ch = (ConfigurationHelper) osgi.getServiceObject(sr2);
        assertThat("Destination: ServicesConfig unavailable", sc, is(notNullValue()));

        ComponentInstance destination1, channel1, service1, adapter1, adapter2, factory;

        adapter1 = ch.newAdapter("adapter1");
        adapter2 = ch.newAdapter("adapter2");
        service1 = ch.newGravityService("service1");
        channel1 = ch.newGravityChannel("channel1", "/uri");
        destination1 = ch.newGravityDestination("destination1", "service1");

        Service ser = sc.findServiceById("service1");
        assertThat("Destination: No service!", ser, is(notNullValue()));

        // Correct start
        assertThat("Destination: No destination!", ser.findDestinationById("destination1"), is(notNullValue()));

        destination1.dispose();

        // Correct stop
        assertThat("Destination: Stop failed!", ser.findDestinationById("destination1"), is(nullValue()));

        destination1 = ch.newGravityDestination("destination1", "service1");
        service1.dispose();

        ser = sc.findServiceById("service1");

        // Check stop of service
        assertThat("Destination: Invalid service state", ser, is(nullValue()));

        service1 = ch.newGravityService("service1", "adapter1");

        ser = sc.findServiceById("service1");
        // Check adapter of destination (service default adapter)
        assertThat("Destination: no adapter 1", ser.findDestinationById("destination1").getAdapter(), is(notNullValue()));
        assertTrue("Destination: Invalid adapter 1", ser.findDestinationById("destination1").getAdapter().getId().equals("adapter1"));

        destination1.dispose();
        destination1 = ch.newGravityDestination("destination1", "service1", "adapter2");

        ser = sc.findServiceById("service1");
        // Check if destination's adapter overload default service adapter
        assertThat("Destination: no adapter 2", ser.findDestinationById("destination1").getAdapter(), is(notNullValue()));
        assertTrue("Destination: Invalid adapter 2", ser.findDestinationById("destination1").getAdapter().getId().equals("adapter2"));

        adapter2.dispose();

        // Check stop of destination (adapter dependency)
        assertThat("Destination: Invalid destination state 2", sc.findDestinationById("MS1", "destination1"), is(nullValue()));

        destination1.dispose();
        service1.dispose();

        service1 = ch.newGraniteService("service1");
        ser = sc.findServiceById("service1");

        // Check start of service
        assertThat("Destination: No service 2!", ser, is(notNullValue()));

        //
        // Granite
        //

        destination1 = ch.newGraniteDestination("destination1", "service1");

        ser = sc.findServiceById("service1");

        // Check start of destination
        assertThat("Destination: Invalid destination state 3", ser.findDestinationById("destination1"), is(notNullValue()));

        destination1.dispose();
        destination1 = ch.newGraniteDestination("destination1", "service1", "factory1", ConfigurationHelper.SCOPE.SESSION);

        ser = sc.findServiceById("service1");

        // Check stop of destination
        assertThat("Destination: Invalid destination state 4", ser.findDestinationById("destination1"), is(nullValue()));
        factory = ch.newFactory("factory1");

        // Check stop of destination
        assertThat("Destination: Invalid destination state 5", ser.findDestinationById("destination1"), is(notNullValue()));

        destination1.dispose();
        factory.dispose();
        service1.dispose();
        channel1.dispose();
        adapter1.dispose();

    }

}
