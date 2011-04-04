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

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import org.granite.logging.Logger;
import org.granite.osgi.util.Converter;
import org.granite.util.XMap;

import java.util.Dictionary;

/**
 * @author Franck WOLFF
 */
@Component
@Provides
public class Channel implements ChannelInterface {

    private static final Logger LOG = Logger.getLogger(Channel.class);

    private static final String LEGACY_XML = "serialization/legacy-xml";
    private static final String LEGACY_COLLECTION = "serialization/legacy-collection";

    protected String id;
    protected String className;
    protected EndPoint endPoint;
    protected XMap properties;

    private final boolean legacyXml;
    private final boolean legacyCollection;

    public Channel(String id, String className, EndPoint endPoint,
                   XMap properties) {
        this.id = id;
        this.className = className;
        this.endPoint = endPoint;
        this.properties = properties;
        this.legacyCollection = Boolean.TRUE.toString().equals(
                properties.get(LEGACY_COLLECTION));
        this.legacyXml = Boolean.TRUE.toString().equals(
                properties.get(LEGACY_XML));
    }

    public String getId() {
        return id;
    }

    public String getClassName() {
        return className;
    }

    public EndPoint getEndPoint() {
        return endPoint;
    }

    public XMap getProperties() {
        return properties;
    }

    public boolean isLegacyXmlSerialization() {
        return legacyXml;
    }

    public boolean isLegacyCollectionSerialization() {
        return legacyCollection;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Static helper.

    public static Channel forElement(XMap element) {
        String id = element.get("@id");
        String className = element.get("@class");

        XMap endPointElt = element.getOne("endpoint");
        if (endPointElt == null)
            throw new RuntimeException(
                    "Excepting a 'endpoint' element in 'channel-definition': " + id);
        EndPoint endPoint = EndPoint.forElement(endPointElt);

        XMap properties = new XMap(element.getOne("properties"));

        return new Channel(id, className, endPoint, properties);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Channel channel = (Channel) o;

        if (id != null ? !id.equals(channel.id) : channel.id != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Channel{" +
                "className='" + className + '\'' +
                ", id='" + id + '\'' +
                ", endPoint=" + endPoint +
                ", properties=" + properties +
                ", legacyXml=" + legacyXml +
                ", legacyCollection=" + legacyCollection +
                '}';
    }

    ///////////////////////////////////////////////////////////////////////////
    // OSGi

    @Requires
    private ServicesConfigInterface servicesConfig;

    public String ENDPOINT_URI;

    public String ENDPOINT_CLASS;

    public Channel() {
        this.id = null;
        this.className = null;
        this.endPoint = null;
        this.properties = new XMap();
        this.legacyCollection = false;
        this.legacyXml = false;
    }

    @Property(name = "ID", mandatory = true)
    private void setId(String id) {
        this.id = id;
    }

    @Property(name = "CLASS", mandatory = false,
              value = "mx.messaging.channels.AMFChannel")
    private void setClass(String className) {
        this.className = className;
    }

    @Property(name = "ENDPOINT_URI", mandatory = true)
    private void setEndPointURI(String epURI) {
        this.ENDPOINT_URI = epURI;
        this.endPoint = new EndPoint(ENDPOINT_URI, ENDPOINT_CLASS);
    }

    @Property(name = "ENDPOINT_CLASS", mandatory = false,
              value = "flex.messaging.endpoints.AMFEndpoint")
    private void setEndPointClass(String epClass) {
        this.ENDPOINT_CLASS = epClass;
        this.endPoint = new EndPoint(ENDPOINT_URI, ENDPOINT_CLASS);
    }


    @Property(name = "PROPERTIES", mandatory = false)
    private void setProperties(Dictionary<String, String> properties) {
        this.properties = Converter.getXMap(properties);
    }

    @Validate
    public void starting() {
        start();
    }

    public void start() {
        LOG.debug("Start Channel:" + this.id);
        servicesConfig.addChannel(this);
    }

    @Invalidate
    public void stopping() {
        stop();
    }

    public void stop() {
        LOG.debug("Stop Channel:" + this.id);
        if (servicesConfig != null) {
            servicesConfig.removeChannel(this.id);
        }
    }
}
