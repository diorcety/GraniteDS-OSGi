package org.granite.gravity.osgi.impl.service;

import flex.messaging.messages.AsyncMessage;
import flex.messaging.messages.CommandMessage;

import org.granite.config.flex.Adapter;

import org.granite.gravity.Channel;
import org.granite.gravity.adapters.ServiceAdapter;
import org.granite.logging.Logger;
import org.granite.osgi.service.GraniteAdapter;


public class OSGiAdapterAbstraction extends ServiceAdapter {

    private static final Logger log = Logger.getLogger(OSGiAdapterAbstraction.class);

    private final GraniteAdapter graniteAdapter;
    private Adapter adapter;

    OSGiAdapterAbstraction(GraniteAdapter graniteAdapter, Adapter adapter) {
        this.graniteAdapter = graniteAdapter;
        this.adapter = adapter;
    }

    public Adapter getAdapter() {
        return adapter;
    }

    @Override
    public Object manage(Channel fromClient, CommandMessage message) {
        return graniteAdapter.manage(fromClient,message);
    }

    @Override
    public Object invoke(Channel fromClient, AsyncMessage message) {
        return graniteAdapter.invoke(fromClient, message);
    }
}
