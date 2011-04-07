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

import org.granite.logging.Logger;
import org.granite.util.XMap;

import java.util.Dictionary;

/**
 * @author Franck WOLFF
 */
public class Channel implements ChannelComponent {

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

    @Override
    public Channel getChannel() {
        return this;
    }

    public boolean isLegacyXmlSerialization() {
        return legacyXml;
    }

    public boolean isLegacyCollectionSerialization() {
        return legacyCollection;
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
}
