/*
 * Copyright (C) 2019 sea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package top.srsea.kash.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lru cache in memory.
 *
 * @param <K> type of key
 * @param <V> type of value
 * @author sea
 */
public class LruCache<K, V> {

    /**
     * Cache container.
     */
    private final LinkedHashMap<K, V> map;

    /**
     * Current cache size.
     */
    private int size;

    /**
     * Max cache size.
     */
    private int maxSize;

    /**
     * Constructs an instance with the specific capacity.
     *
     * @param maxSize capacity
     */
    public LruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<>(0, 0.75f, true);
    }

    /**
     * Resize this cache with the specific capacity.
     *
     * @param maxSize capacity, must be positive
     */
    public void resize(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }

        synchronized (this) {
            this.maxSize = maxSize;
        }
        trimToSize(maxSize);
    }

    /**
     * Returns the value to which the specified key mapped,
     * or {@code null} if this cache contains no mapping for the key.
     *
     * @param key the key of value
     * @return cached value, or {@code null}
     * @throws NullPointerException if the key is null
     */
    public final V get(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        V mapValue;
        synchronized (this) {
            mapValue = map.get(key);
            if (mapValue != null) {
                return mapValue;
            }
        }
        return null;
    }

    /**
     * Puts a pair of key, value into this cache.
     *
     * @param key   key of cache
     * @param value value of key
     * @return previous value, which was replaced
     * @throws NullPointerException if the key or value are null
     */
    public final V put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("key == null || value == null");
        }

        V previous;
        synchronized (this) {
            ++size;
            previous = map.put(key, value);
            if (previous != null) {
                --size;
            }
        }

        trimToSize(maxSize);
        return previous;
    }

    /**
     * Trims the cache to the specific size.
     *
     * @param maxSize the specific size to trim
     */
    private void trimToSize(int maxSize) {
        while (true) {
            K key;
            synchronized (this) {
                if (size < 0 || (map.isEmpty() && size != 0)) {
                    throw new IllegalStateException(getClass().getName()
                            + ".sizeOf() is reporting inconsistent results!");
                }

                if (size <= maxSize) {
                    break;
                }


                Map.Entry<K, V> toEvict = null;
                for (Map.Entry<K, V> entry : map.entrySet()) {
                    toEvict = entry;
                }


                if (toEvict == null) {
                    break;
                }

                key = toEvict.getKey();
                map.remove(key);
                --size;
            }
        }
    }

    /**
     * Removes an item in this cache.
     *
     * @param key key of cache
     * @return value removed.
     */
    public final V remove(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        V previous;
        synchronized (this) {
            previous = map.remove(key);
            if (previous != null) {
                --size;
            }
        }

        return previous;
    }

    /**
     * Clears all cache.
     */
    public final void evictAll() {
        trimToSize(-1);
    }

    /**
     * Gets the cache size.
     *
     * @return cache size
     */
    public synchronized final int size() {
        return size;
    }

    /**
     * Gets the max size of this cache.
     *
     * @return max size of this cache
     */
    public synchronized final int maxSize() {
        return maxSize;
    }
}
