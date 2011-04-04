package org.granite.osgi.impl;


import org.granite.osgi.OSGiGraniteContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.*;

public class HttpGraniteContext extends OSGiGraniteContextImpl {

    private static final String SESSION_LOCK_KEY = HttpGraniteContext.class.getName() + ".LOCK";

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    private Map<String, String> initialisationMap = null;
    private SessionMap sessionMap = null;
    private RequestMap requestMap = null;


    public static HttpGraniteContext createThreadIntance(
            OSGiGraniteContext context,
            HttpServletRequest request,
            HttpServletResponse response) {

        HttpGraniteContext graniteContext = new HttpGraniteContext
                (context, request, response);
        setCurrentInstance(graniteContext);
        return graniteContext;
    }

    private HttpGraniteContext(
            OSGiGraniteContext context,
            HttpServletRequest request,
            HttpServletResponse response) {

        super(context.getGraniteConfig(), context.getServicesConfig());
        this.applicationMap = context.getApplicationMap();
        this.request = request;
        this.response = response;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public HttpSession getSession(boolean create) {
        return request.getSession(create);
    }

    public HttpSession getSession() {
        return request.getSession(true);
    }

    @Override
    public synchronized Object getSessionLock() {
        Object lock = request.getSession(true).getAttribute(SESSION_LOCK_KEY);
        if (lock == null) {
            lock = new Boolean(true);
            request.getSession(true).setAttribute(SESSION_LOCK_KEY, lock);
        }
        return lock;
    }

    @Override
    public Map<String, String> getInitialisationMap() {
        if (initialisationMap == null)
            initialisationMap = new HashMap<String, String>();
        return initialisationMap;
    }

    @Override
    public Map<String, Object> getSessionMap() {
        return getSessionMap(true);
    }

    @Override
    public Map<String, Object> getSessionMap(boolean create) {
        if (sessionMap == null && (create || request.getSession(false) != null))
            sessionMap = new SessionMap(request);
        return sessionMap;
    }

    @Override
    public Map<String, Object> getRequestMap() {
        if (requestMap == null)
            requestMap = new RequestMap(request);
        return requestMap;
    }
}

abstract class BaseContextMap<T, U> extends AbstractMap<T, U> {

    protected static final String KEY_STRING_ERROR = "Key should be a non null String: ";

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends T, ? extends U> t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public U remove(Object key) {
        throw new UnsupportedOperationException();
    }

    static class Entry<T, U> implements Map.Entry<T, U> {

        private final T key;
        private final U value;

        Entry(T key, U value) {
            this.key = key;
            this.value = value;
        }

        public T getKey() {
            return key;
        }

        public U getValue() {
            return value;
        }

        public U setValue(U value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            return ((key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode()));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;

            if (obj == null || !(obj instanceof Map.Entry<?, ?>))
                return false;

            Map.Entry<?, ?> input = (Map.Entry<?, ?>) obj;
            Object inputKey = input.getKey();
            Object inputValue = input.getValue();

            if (inputKey == key || (inputKey != null && inputKey.equals(key))) {
                if (inputValue == value || (inputValue != null && inputValue.equals(
                        value)))
                    return true;
            }
            return false;
        }
    }
}

class InitialisationMap extends BaseContextMap<String, String> {

    private ServletContext servletContext = null;

    InitialisationMap(ServletContext servletContext) {
        if (servletContext == null)
            throw new NullPointerException("servletContext is null");
        this.servletContext = servletContext;
    }

    @Override
    public String get(Object key) {
        if (!(key instanceof String))
            return null;
        return servletContext.getInitParameter(key.toString());
    }

    @Override
    public String put(String key, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Map.Entry<String, String>> entrySet() {
        Set<Map.Entry<String, String>> entries = new HashSet<Map.Entry<String, String>>();
        for (Enumeration<?> e = servletContext.getInitParameterNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            entries.add(new Entry<String, String>(key,
                                                  servletContext.getInitParameter(
                                                          key)));
        }
        return entries;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof InitialisationMap))
            return false;
        return super.equals(obj);
    }
}

class SessionMap extends BaseContextMap<String, Object> {

    private HttpServletRequest request = null;

    SessionMap(HttpServletRequest request) {
        if (request == null)
            throw new NullPointerException("request is null");
        this.request = request;
    }

    @Override
    public Object get(Object key) {
        if (!(key instanceof String))
            return null;
        return getSession().getAttribute(key.toString());
    }

    @Override
    public Object put(String key, Object value) {
        if (key == null)
            throw new IllegalArgumentException(KEY_STRING_ERROR + key);
        HttpSession session = getSession();
        Object result = session.getAttribute(key);
        session.setAttribute(key, value);
        return result;
    }

    @Override
    public Object remove(Object key) {
        if (!(key instanceof String))
            return null;
        HttpSession session = getSession();
        Object result = session.getAttribute(key.toString());
        session.removeAttribute(key.toString());
        return result;
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        Set<Map.Entry<String, Object>> entries = new HashSet<Map.Entry<String, Object>>();
        HttpSession session = getSession();
        for (Enumeration<?> e = session.getAttributeNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            entries.add(
                    new Entry<String, Object>(key, session.getAttribute(key)));
        }
        return entries;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SessionMap))
            return false;
        return super.equals(obj);
    }

    private HttpSession getSession() {
        return request.getSession(true);
    }
}

class RequestMap extends BaseContextMap<String, Object> {

    private HttpServletRequest request = null;

    RequestMap(HttpServletRequest request) {
        if (request == null)
            throw new NullPointerException("request is null");
        this.request = request;
    }

    @Override
    public Object get(Object key) {
        if (!(key instanceof String))
            return null;
        return request.getAttribute(key.toString());
    }

    @Override
    public Object put(String key, Object value) {
        if (key == null)
            throw new IllegalArgumentException(KEY_STRING_ERROR + key);
        Object result = request.getAttribute(key);
        request.setAttribute(key, value);
        return result;
    }

    @Override
    public Object remove(Object key) {
        if (!(key instanceof String))
            return null;
        Object result = request.getAttribute(key.toString());
        request.removeAttribute(key.toString());
        return result;
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        Set<Map.Entry<String, Object>> entries = new HashSet<Map.Entry<String, Object>>();
        for (Enumeration<?> e = request.getAttributeNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            entries.add(
                    new Entry<String, Object>(key, request.getAttribute(key)));
        }
        return entries;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof RequestMap))
            return false;
        return super.equals(obj);
    }
}
