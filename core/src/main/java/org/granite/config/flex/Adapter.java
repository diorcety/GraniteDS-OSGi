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
public class Adapter implements AdapterComponent {

    private static final Logger LOG = Logger.getLogger(Adapter.class);

    protected String id;
    protected String className;
    protected XMap properties;

    public Adapter(String id, String className, XMap properties) {
        this.id = id;
        this.className = className;
        this.properties = properties;
    }

    public String getId() {
        return id;
    }

    public String getClassName() {
        return className;
    }

    public XMap getProperties() {
        return properties;
    }

    @Override
    public Adapter getAdapter() {
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Static helper.

    public static Adapter forElement(XMap element) {
        String id = element.get("@id");
        String className = element.get("@class");
        XMap properties = new XMap(element.getOne("properties"));

        return new Adapter(id, className, properties);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Adapter adapter = (Adapter) o;

        if (id != null ? !id.equals(adapter.id) : adapter.id != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Adapter{" +
                "className='" + className + '\'' +
                ", id='" + id + '\'' +
                ", properties=" + properties +
                '}';
    }

    ///////////////////////////////////////////////////////////////////////////
    // OSGi

    protected Adapter() {
        this.id = null;
        this.className = null;
        this.properties = new XMap();
    }

    @Property(name = "ID", mandatory = true)
    private void setId(String id) {
        this.id = id;
    }

    @Property(name = "CLASS", mandatory = true)
    private void setClass(String className) {
        this.className = className;
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
        LOG.debug("Start Adapter:" + this.id);
    }

    @Invalidate
    public void stopping() {
        stop();
    }

    public void stop() {
        LOG.debug("Stop Adapter:" + this.id);
    }
}
