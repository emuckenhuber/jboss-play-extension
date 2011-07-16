/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.play.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;

import play.cache.CacheImpl;

/**
 * {@code CacheImpl} delegating to an infinispan cache.
 *
 * @author Emanuel Muckenhuber
 */
class InfinispanCacheImpl implements CacheImpl {

    private final Cache<String, Object> cacheImpl;

    InfinispanCacheImpl(final Cache<String, Object> delegate) {
        this.cacheImpl = delegate;
    }

    public void add(String key, Object value, int expiration) {
        cacheImpl.putIfAbsentAsync(key, value, expiration, TimeUnit.SECONDS);
    }

    public boolean safeAdd(String key, Object value, int expiration) {
        return cacheImpl.putIfAbsent(key, value, expiration, TimeUnit.SECONDS) == null;
    }

    public void set(String key, Object value, int expiration) {
        cacheImpl.putAsync(key, value, expiration, TimeUnit.SECONDS);
    }

    public boolean safeSet(String key, Object value, int expiration) {
        return cacheImpl.put(key, value, expiration, TimeUnit.SECONDS) != null;
    }

    public void replace(String key, Object value, int expiration) {
        cacheImpl.replaceAsync(key, value, expiration, TimeUnit.SECONDS);
    }

    public boolean safeReplace(String key, Object value, int expiration) {
        return cacheImpl.replace(key, value, expiration, TimeUnit.SECONDS) != null;
    }

    public Object get(String key) {
        return cacheImpl.get(key);
    }

    public Map<String, Object> get(String[] keys) {
        final Map<String, Object> map = new HashMap<String, Object>(keys.length);
        for(final String key : keys) {
            map.put(key, get(key));
        }
        return map;
    }

    public long incr(String key, int by) {
        for(;;) {
            Object o = cacheImpl.get(key);
            if (o == null) {
                return -1;
            }
            long newValue = ((Number) o).longValue() + by;
            if(cacheImpl.replace(key, o, newValue)) {
                return newValue;
            }
        }
    }

    public long decr(String key, int by) {
        for(;;) {
            Object o = cacheImpl.get(key);
            if (o == null) {
                return -1;
            }
            long newValue = ((Number) o).longValue() - by;
            if(cacheImpl.replace(key, o, newValue)) {
                return newValue;
            }
        }
    }

    public void clear() {
        cacheImpl.clear();
    }

    public void delete(String key) {
        cacheImpl.removeAsync(key);
    }

    public boolean safeDelete(String key) {
        return cacheImpl.remove(key) != null;
    }

    public void stop() {
        cacheImpl.stop();
    }
}
