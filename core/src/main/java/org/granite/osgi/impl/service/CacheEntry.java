package org.granite.osgi.impl.service;

import java.util.Map;

public class CacheEntry {
    public Map<String, Object> cache;
    public String entry;

    public CacheEntry(Map<String, Object> cache, String entry) {
        this.cache = cache;
        this.entry = entry;
    }
}
