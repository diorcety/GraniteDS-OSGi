package org.granite.osgi.impl;

import java.util.Map;

public class CacheEntry {
    public Map<String, Object> cache;
    public String entry;

    CacheEntry(Map<String, Object> cache, String entry) {
        this.cache = cache;
        this.entry = entry;
    }
}
