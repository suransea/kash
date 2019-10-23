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

public class LruCache<K, V> {
    private final LinkedHashMap<K, V> map;

    private int size;
    private int maxSize;

    public LruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<>(0, 0.75f, true);
    }

    public void resize(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }

        synchronized (this) {
            this.maxSize = maxSize;
        }
        trimToSize(maxSize);
    }


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


    public final void evictAll() {
        trimToSize(-1);
    }


    public synchronized final int size() {
        return size;
    }


    public synchronized final int maxSize() {
        return maxSize;
    }
}
