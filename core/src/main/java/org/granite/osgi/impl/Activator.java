package org.granite.osgi.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
    static BundleContext bundleContext = null;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        this.bundleContext = bundleContext;
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        this.bundleContext = null;
    }

    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }
}
