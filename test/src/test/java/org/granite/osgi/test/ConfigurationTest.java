package org.granite.osgi.test;


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
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

import java.io.*;

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;
import static org.junit.Assert.assertThat;
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
        try {
            Thread.sleep(500);
            Logging.load(context);
        } catch (Exception ex) {
            System.out.println(ex);
        }

        osgi = new OSGiHelper(context);
        ipojo = new IPOJOHelper(context);

        try {
            Thread.sleep(4000);

        } catch (Exception ex) {

        }

        logger.debug("Start!");
    }

    public void tearDown() {
        osgi.dispose();
        ipojo.dispose();
    }

    @Test
    public void withBC(BundleContext context) throws IOException {
        assertThat(context, is(notNullValue()));

        setup(context);
        tearDown();
    }
}
