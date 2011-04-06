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

package org.granite.config.flex;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.logging.Logger;
import org.granite.messaging.service.security.DestinationSecurizer;
import org.granite.util.ClassUtil;
import org.granite.util.XMap;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

/**
 * @author Franck WOLFF
 */
@Component
public class Destination implements Serializable, DestinationComponent {

    private static final Logger LOG = Logger.getLogger(Destination.class);

    private static final long serialVersionUID = 1L;

    private static final String SECURIZER_PROPERTY_KEY = "securizer";

    protected String id;
    protected List<String> channelRefs;
    protected XMap properties;
    protected List<String> roles;
    protected Adapter adapter;
    protected Class<?> scannedClass;
    protected DestinationSecurizer securizer;

    public Destination(String id, List<String> channelRefs, XMap properties,
                       List<String> roles, Adapter adapter,
                       Class<?> scannedClass) {
        this.id = id;
        this.channelRefs = new ArrayList<String>(channelRefs);
        this.properties = properties;
        this.roles = (roles != null ? new ArrayList<String>(roles) : null);
        this.adapter = adapter;
        this.scannedClass = scannedClass;

        final String securizerClassName = properties.get(
                SECURIZER_PROPERTY_KEY);
        if (securizerClassName != null) {
            try {
                this.securizer = ClassUtil.newInstance(
                        securizerClassName.trim(), DestinationSecurizer.class);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Could not instantiate securizer: " + securizerClassName,
                        e);
            }
        } else
            this.securizer = null;
    }

    public String getId() {
        return id;
    }

    public List<String> getChannelRefs() {
        return channelRefs;
    }

    public XMap getProperties() {
        return properties;
    }

    public boolean isSecured() {
        return roles != null;
    }

    public List<String> getRoles() {
        return roles;
    }

    @Override
    public Destination getDestination() {
        return this;
    }

    public Adapter getAdapter() {
        return adapter;
    }

    public Class<?> getScannedClass() {
        return scannedClass;
    }

    public DestinationSecurizer getSecurizer() {
        return securizer;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Static helper.

    public static Destination forElement(XMap element, Adapter defaultAdapter,
                                         Map<String, Adapter> adaptersMap) {
        String id = element.get("@id");

        List<String> channelRefs = new ArrayList<String>();
        for (XMap channel : element.getAll("channels/channel[@ref]"))
            channelRefs.add(channel.get("@ref"));

        XMap properties = new XMap(element.getOne("properties"));

        List<String> rolesList = null;
        if (element.containsKey("security/security-constraint/roles/role")) {
            rolesList = new ArrayList<String>();
            for (XMap role : element.getAll(
                    "security/security-constraint/roles/role"))
                rolesList.add(role.get("."));
        }

        XMap adapter = element.getOne("adapter[@ref]");
        Adapter adapterRef = adapter != null && adaptersMap != null
                ? adaptersMap.get(adapter.get("@ref"))
                : defaultAdapter;

        return new Destination(id, channelRefs, properties, rolesList,
                               adapterRef, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Destination that = (Destination) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Destination{" +
                "adapter=" + adapter +
                ", id='" + id + '\'' +
                ", channelRefs=" + channelRefs +
                ", properties=" + properties +
                ", roles=" + roles +
                ", scannedClass=" + scannedClass +
                ", securizer=" + securizer +
                '}';
    }

    ///////////////////////////////////////////////////////////////////////////
    // OSGi


    @Property(mandatory = true)
    public String SERVICE;

    @Property(mandatory = true)
    public Collection<String> CHANNEL_LIST;

    @Property(mandatory = false)
    public String ADAPTER;

    //
    private boolean state = false;

    private boolean started = false;

    private ServiceComponent service;

    protected Destination() {
        this.id = null;
        this.channelRefs = new ArrayList<String>();
        this.properties = new XMap();
        this.roles = new ArrayList<String>();
        this.adapter = null;
        this.scannedClass = null;
        this.securizer = null;
    }

    @Property(name = "ID", mandatory = true)
    private void setId(String id) {
        this.id = id;
    }

    @Property(name = "PROPERTIES", mandatory = false)
    private void setProperties(Dictionary<String, String> properties) {
        this.properties = new XMap(properties);
    }


    @Bind(aggregate = true, optional = true)
    private void bindService(ServiceComponent service) {
        if (service.getId() == this.SERVICE) {
            this.service = service;
            checkState();
        }
    }

    @Unbind
    private void unbindService(ServiceComponent service) {
        if (service.getId() == this.SERVICE) {
            this.service = null;
            checkState();
        }
    }

    @Bind(aggregate = true, optional = true)
    private void bindAdapter(AdapterComponent adapter) {
        if (adapter.getId() == this.ADAPTER) {
            this.adapter = (Adapter) adapter;  //HACK
            checkState();
        }
    }

    @Unbind
    private void unbindAdapter(AdapterComponent adapter) {
        if (adapter.getId() == this.ADAPTER) {
            this.adapter = null;
            checkState();
        }
    }

    @Bind(aggregate = true, optional = true)
    private void bindChannel(ChannelComponent channel) {
        if (this.CHANNEL_LIST.contains(channel.getId())) {
            this.channelRefs.add(channel.getId());
            checkState();
        }
    }

    @Unbind
    private void unbindChannel(ChannelComponent channel) {
        if (this.CHANNEL_LIST.contains(channel.getId())) {
            this.channelRefs.remove(channel.getId());
            checkState();
        }
    }

    private void checkState() {
        boolean new_state;
        if (started && service != null && this.channelRefs.size() > 0) {
            new_state = true;
        } else {
            new_state = false;
        }
        if (new_state != this.state) {
            if (new_state)
                start();
            else
                stop();

            this.state = new_state;
        }
    }

    @Validate
    public void starting() {
        started = true;
        checkState();
    }

    public void start() {
        LOG.debug("Start Destination:" + this.id);
        service.addDestination(this);
    }

    @Invalidate
    public void stopping() {
        if (this.state) {
            stop();
            this.state = false;
        }
        started = false;
    }

    public void stop() {
        LOG.debug("Stop Destination:" + this.id);
        if (service != null) {
            service.removeDestination(this.id);
        }
    }
}
