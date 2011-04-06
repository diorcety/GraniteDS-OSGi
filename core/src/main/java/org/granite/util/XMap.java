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

package org.granite.util;

import org.granite.logging.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.*;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;

/**
 * Utility class that makes XML fragment tree manipulation easier.
 * <br />
 * This class relies on JDK DOM & XPath built-in implementations.
 *
 * @author Franck WOLFF
 */
public class XMap implements Serializable {

	private static final Logger log = Logger.getLogger(XMap.class);

	private static final long serialVersionUID = 1L;

	protected static final String DEFAULT_ROOT_NAME = "root";

	/**
	 * An empty and unmodifiable XMap instance.
	 */
	public static final XMap EMPTY_XMAP = new XMap(null, null, false) {

		private static final long serialVersionUID = 1L;

		@Override
		public String put(String key, String value) {
			throw new RuntimeException("Immutable XMap");
		}

		@Override
		public String remove(String key) {
			throw new RuntimeException("Immutable XMap");
		}
	};

	private transient DOM dom = null;
	private transient Element root = null;

	/**
	 * Constructs a new XMap instance.
	 */
	public XMap() {
		this(null, null, false);
	}

    public XMap(Dictionary<String, String> dictionary) {
        Enumeration enumeration = dictionary.keys();
        while (enumeration.hasMoreElements()) {
            String id = (String) enumeration.nextElement();
            put(id, (String) dictionary.get(id));
        }
    }

	/**
	 * Constructs a new XMap instance.
	 *
	 * @param root the name of the root element (may be null).
	 */
	public XMap(String root) {
		if (root != null) {
			this.dom = new DOM();
			this.root = dom.newDocument(root).getDocumentElement();
		}
	}

	/**
	 * Constructs a new XMap instance from an XML input stream.
	 *
	 * @param input an XML input stream.
	 */
	public XMap(InputStream input) throws IOException, SAXException {
		this.dom = new DOM();
		this.root = dom.loadDocument(input).getDocumentElement();
	}

	/**
	 * Constructs a new XMap instance from an XML input stream.
	 *
	 * @param input an XML input stream.
	 */
	public XMap(InputStream input, EntityResolver resolver) throws IOException, SAXException {
		this.dom = new DOM();
		this.root = dom.loadDocument(input, resolver).getDocumentElement();
	}


	/**
	 * Constructs a new XMap instance.
	 *
	 * @param root a DOM element (may be null).
	 */
	public XMap(Element root) {
		this(null, root, true);
	}

	/**
	 * Constructs a new XMap instance based on an existing XMap and clone its content.
	 *
	 * @param map the map to duplicate (root element is cloned so modification to this
	 * 		new instance won't modify the original XMap).
	 */
	public XMap(XMap map) {
		this((map == null ? null : map.dom), (map == null ? null : map.root), true);
	}

	/**
	 * Constructs a new XMap instance.
	 *
	 * @param root the root element (may be null).
	 * @param clone should we clone the root element (prevent original node modification).
	 */
	protected XMap(DOM dom, Element root, boolean clone) {
		this.dom = dom;
		this.root = (clone && root != null ? (Element)root.cloneNode(true) : root);

	}

	/**
	 * Allows direct manipulation of the root element.
	 *
	 * @return the root element of this XMap instance.
	 */
	public Element getRoot() {
		return root;
	}

	/**
	 * Returns the DOM instance of this XMap (one is created if it is null).
	 *
	 * @return the DOM instance of this XMap.
	 */
	protected DOM getDom() {
		if (dom == null)
			dom = new DOM();
		return dom;
	}

	/**
	 * Returns true if the supplied key XPath expression matches at least one element, attribute
	 * or text in the root element of this XMap.
	 *
	 * @param key an XPath expression.
	 * @return true if the supplied key XPath expression matches at least one element, attribute
	 * 		or text in the root element of this XMap, false otherwise.
	 * @throws RuntimeException if the XPath expression isn't correct.
	 */
	public boolean containsKey(String key) {
		if (root == null)
			return false;
		try {
			Node result = getDom().selectSingleNode(root, key);
			return (
				result != null && (
					result.getNodeType() == Node.ELEMENT_NODE ||
					result.getNodeType() == Node.TEXT_NODE ||
					result.getNodeType() == Node.ATTRIBUTE_NODE
				)
			);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the text value of the element (or attribute or text) that matches the supplied
	 * XPath expression.
	 *
	 * @param key an XPath expression.
	 * @return the text value of the matched element or null if the element does not exist or have
	 * 		no value.
	 * @throws RuntimeException if the XPath expression isn't correct.
	 */
	public String get(String key) {
		if (root == null)
			return null;
		try {
			return getDom().getNormalizedValue(getDom().selectSingleNode(root, key));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public <T> T get(String key, Class<T> clazz, T defaultValue) {
		return get(key, clazz, defaultValue, false, true);
	}

	public <T> T get(String key, Class<T> clazz, T defaultValue, boolean required, boolean warn) {

		String sValue = get(key);

    	if (required && sValue == null)
    		throw new RuntimeException(key + " value is required in XML file:\n" + toString());

        Object oValue = defaultValue;

        boolean unsupported = false;
        if (sValue != null) {
	        try {
	        	if (clazz == String.class)
	        		oValue = sValue;
	        	else if (clazz == Integer.class || clazz == Integer.TYPE)
	        		oValue = Integer.valueOf(sValue);
	        	else if (clazz == Long.class || clazz == Long.TYPE)
	        		oValue = Long.valueOf(sValue);
	        	else if (clazz == Boolean.class || clazz == Boolean.TYPE) {
	        		if (!Boolean.TRUE.toString().equalsIgnoreCase(sValue) && !Boolean.FALSE.toString().equalsIgnoreCase(sValue))
	        			throw new NumberFormatException(sValue);
	        		oValue = Boolean.valueOf(sValue);
	        	}
	        	else if (clazz == Double.class || clazz == Double.TYPE)
	        		oValue = Double.valueOf(sValue);
	        	else if (clazz == Float.class || clazz == Float.TYPE)
	        		oValue = Float.valueOf(sValue);
	        	else if (clazz == Short.class || clazz == Short.TYPE)
	        		oValue = Short.valueOf(sValue);
	        	else if (clazz == Byte.class || clazz == Byte.TYPE)
	        		oValue = Byte.valueOf(sValue);
	        	else
	        		unsupported = true;
	        }
	        catch (Exception e) {
	        	if (warn)
	        		log.warn(e, "Illegal %s value for %s=%s (using default: %s)", clazz.getSimpleName(), key, sValue, defaultValue);
	        }
        }

        if (unsupported)
        	throw new UnsupportedOperationException("Unsupported value type: " + clazz.getName());

        @SuppressWarnings("unchecked")
        T tValue = (T)oValue;

    	return tValue;
	}

	/**
	 * Returns a list of XMap instances with all elements that match the
	 * supplied XPath expression. Note that XPath result nodes that are not instance of
	 * Element are ignored. Note also that returned XMaps contain original child elements of
	 * the root element of this XMap so modifications made to child elements affect this XMap
	 * instance as well.
	 *
	 * @param key an XPath expression.
	 * @return an unmodifiable list of XMap instances.
	 * @throws RuntimeException if the XPath expression isn't correct.
	 */
	public List<XMap> getAll(String key) {
		if (root == null)
			return new ArrayList<XMap>(0);
		try {
			List<Node> result = dom.selectNodes(root, key);
			List<XMap> xMaps = new ArrayList<XMap>(result.size());
			for (Node node : result) {
				if (node.getNodeType() == Node.ELEMENT_NODE)
					xMaps.add(new XMap(this.dom, (Element)node, false));
			}
			return xMaps;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns a new XMap instance with the first element that matches the
	 * supplied XPath expression or null if this XMap root element is null, or if XPath evaluation
	 * result is null, or this result is not an Element. Returned XMap contains original child element of
	 * the root element of this XMap so modifications made to the child element affect this XMap
	 * instance as well.
	 *
	 * @param key an XPath expression.
	 * @return a single new XMap instance.
	 * @throws RuntimeException if the XPath expression isn't correct.
	 */
	public XMap getOne(String key) {
		if (root == null)
			return null;
		try {
			Node node = getDom().selectSingleNode(root, key);
			if (node == null || node.getNodeType() != Node.ELEMENT_NODE)
				return null;
			return new XMap(getDom(), (Element)node, false);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates or updates the text value of the element (or text or attribute) matched by
	 * the supplied XPath expression. If the matched element (or text or attribute) does not exist,
	 * it is created with the last segment of the XPath expression (but its parent must already exist).
	 *
	 * @param key an XPath expression.
	 * @param value the value to set (may be null).
	 * @return the previous value of the matched element (may be null).
	 * @throws RuntimeException if the root element of this XMap is null, if the XPath expression is not valid,
	 * 		or (creation case) if the parent node does not exist or is not an element instance.
	 */
	public String put(String key, String value) {
		if (root == null)
			root = getDom().newDocument(DEFAULT_ROOT_NAME).getDocumentElement();
		try {
			Node selectResult = getDom().selectSingleNode(root, key);
			if (selectResult != null)
				return getDom().setValue(selectResult, value);
		} catch(RuntimeException e) {
			throw e;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}

		Element parent = root;
		String name = key;

		int iLastSlash = key.lastIndexOf('/');
		if (iLastSlash != -1) {
			name = key.substring(iLastSlash + 1);
			Node selectResult = null;
			try {
				selectResult = getDom().selectSingleNode(root, key.substring(0, iLastSlash));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			if (selectResult == null)
				throw new RuntimeException("Parent node does not exist: " + key.substring(0, iLastSlash));
			if (!(selectResult instanceof Element))
				throw new RuntimeException("Parent node must be an Element: " + key.substring(0, iLastSlash) + " -> " + selectResult);
			parent = (Element)selectResult;
		}

		if (name.length() > 0 && name.charAt(0) == '@')
			parent.setAttribute(name.substring(1), value);
		else
			getDom().newElement(parent, name, value);

		return null;
	}

	/**
	 * Removes the element, text or attribute that matches the supplied XPath expression.
	 *
	 * @param key  an XPath expression.
	 * @return the previous value of the matched node if any.
	 * @throws RuntimeException if the XPath expression isn't valid.
	 */
	public String remove(String key) {
		if (root == null)
			return null;
		try {
			Node node = getDom().selectSingleNode(root, key);
			if (node != null) {
				String value = getDom().getNormalizedValue(node);
				if (node.getNodeType() == Node.ATTRIBUTE_NODE)
					((Attr)node).getOwnerElement().removeAttribute(node.getNodeName());
				else
					node.getParentNode().removeChild(node);
				return value;
			}
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	/**
	 * Returns a "pretty" XML representation of the root element of this XMap (may be null).
	 *
	 * @return a "pretty" XML representation of the root element of this XMap (may be null).
	 */
	@Override
	public String toString() {
		return getDom().toString(root);
	}

	/**
	 * Write java.io.Serializable method.
	 *
	 * @param out the ObjectOutputStream where to write this XMap.
	 * @throws java.io.IOException if writing fails.
	 */
	private void writeObject(ObjectOutputStream out) throws IOException {
		if (root == null)
			out.writeInt(0);
		else {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			try {
				getDom().saveDocument(root.getOwnerDocument(), output);
			} catch (TransformerException e) {
				IOException ioe = new IOException("Could not serialize this XMap");
				ioe.initCause(e);
				throw ioe;
			}
			out.writeInt(output.size());
			out.write(output.toByteArray());
		}
	}

	/**
	 * Read java.io.Serializable method.
	 *
	 * @param in the ObjectInputStream from which to read this XMap.
	 * @throws java.io.IOException if readind fails.
	 */
	@SuppressWarnings("unused")
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		int size = in.readInt();
		if (size > 0) {
			byte[] content = new byte[size];
			in.readFully(content);
			Document doc = null;
			try {
				doc = getDom().loadDocument(new ByteArrayInputStream(content));
			} catch (Exception e) {
				IOException ioe = new IOException("Could not deserialize this XMap");
				ioe.initCause(e);
				throw ioe;
			}
			if (doc != null && doc.getDocumentElement() != null)
				this.root = doc.getDocumentElement();
		}
	}
}
