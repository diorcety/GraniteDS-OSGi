package org.granite.osgi.service;

public interface GraniteFactory {
    public String getId();

    Object newInstance();
}
