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

package top.srsea.kash;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import top.srsea.kash.pojo.CacheItem;
import top.srsea.kash.pojo.Metadata;
import top.srsea.kash.util.FileHelper;
import top.srsea.kash.util.LruCache;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

/**
 * DiskCache.
 *
 * <p>A synchronous disk cache, one item mapped one file,
 * metadata files describe overview of the cache.
 * Optionally, a built-in LruCache in memory can be enabled
 * to build a L2 cache.
 *
 * @author sea
 * @see LruCache
 */
public class DiskCache {
    private static final Logger logger = Logger.getLogger("DiskCache"); //jdk logger

    /**
     * The filename of metadata file.
     */
    private static final String FILENAME_METADATA = "kash-metadata.json";

    /**
     * The default name of cache.
     */
    private static final String DEFAULT_CACHE_NAME = "kash";

    /**
     * Charset for converting between bytes and string.
     */
    private Charset charset = StandardCharsets.UTF_8;

    /**
     * The metadata of this cache.
     */
    private Metadata metadata;

    /**
     * The name of this cache, default is {@link DiskCache#DEFAULT_CACHE_NAME}.
     */
    private String name;

    /**
     * The parent path of this cache, default is {@code "${HOME}/.cache"}, or "." when ${HOME} is empty.
     */
    private File cachePath;

    /**
     * The metadata file object.
     */
    private File metadataFile;

    /**
     * The cache item map build from metadata.
     */
    private Map<String, CacheItem> cacheItemMap;

    /**
     * The serializer to converting between objects and bytes.
     */
    private Serializer serializer;

    /**
     * A value whether to use memory cache.
     */
    private boolean enableMemoryCache;

    /**
     * Max size of memory cache.
     */
    private int maxMemoryCacheCount;

    /**
     * Don't cache bytes when cache item size greater than this value.
     * This value is useless when memory cache disabled.
     */
    private int maxMemoryCacheSingleSize;

    /**
     * Memory cache, {@code null} when memory cache disabled.
     */
    private LruCache<String, byte[]> memoryCache;

    /**
     * Constructs an instance with builder.
     *
     * @param builder the specific builder
     */
    private DiskCache(Builder builder) {
        File path = builder.path;
        name = builder.name;
        serializer = builder.serializer;
        enableMemoryCache = builder.enableMemoryCache;
        maxMemoryCacheCount = builder.maxMemoryCacheCount;
        maxMemoryCacheSingleSize = builder.maxMemoryCacheSingleSize;
        if (path == null) {
            String homeEnv = System.getenv("HOME");
            if (!StringUtils.isEmpty(homeEnv)) {
                path = new File(new File(homeEnv), ".cache");
            } else {
                path = new File(".");
            }
        }
        if (name == null) name = DEFAULT_CACHE_NAME;
        if (serializer == null) serializer = new GsonSerializer();
        cachePath = new File(path, name);
        metadataFile = new File(cachePath, FILENAME_METADATA);
        initialize();
    }

    /**
     * Initializes this cache, you need call this method
     * before you use this cache object.
     */
    private void initialize() {
        if (metadataFile.exists()) {
            String data = new String(readFile(metadataFile), charset);
            metadata = new Gson().fromJson(data, Metadata.class);
            metadata.setItems(Collections.synchronizedList(metadata.getItems()));
        } else {
            if (!cachePath.exists() && !cachePath.mkdirs()) {
                throw new RuntimeException("cannot mkdirs.");
            }
            metadata = new Metadata();
            metadata.setName(name);
            metadata.setItems(Collections.synchronizedList(new ArrayList<CacheItem>()));
            writeMetadata();
            logger.info("new metadata file created.");
        }
        cacheItemMap = Collections.synchronizedMap(new HashMap<String, CacheItem>(metadata.getItems().size()));
        for (Iterator<CacheItem> it = metadata.getItems().iterator(); it.hasNext(); ) {
            CacheItem item = it.next();
            if (item.getExpiredTime() != null && System.currentTimeMillis() >= item.getExpiredTime()) {
                it.remove();
                FileHelper.delete(fileOfKey(item.getKey()));
                continue;
            }
            cacheItemMap.put(item.getKey(), item);
        }
        writeMetadata();
        if (enableMemoryCache) {
            memoryCache = new LruCache<>(maxMemoryCacheCount);
        }
        logger.info(String.format("DiskCache %s initialized.", name));
    }

    /**
     * Evicts all cache.
     */
    public void evictAll() {
        metadata.getItems().clear();
        cacheItemMap.clear();
        if (enableMemoryCache) memoryCache.evictAll();
        FileHelper.deleteUnder(cachePath);
    }

    /**
     * Removes the cache of the key from disk.
     *
     * @param key key of cache to remove
     */
    public void remove(String key) {
        CacheItem item = cacheItemMap.get(key);
        if (item == null) return;
        remove(item);
    }

    /**
     * Gets current cache size.
     *
     * @return size of cache
     */
    public int size() {
        return metadata.getItems().size();
    }

    /**
     * Returns cache as bytes.
     * If memory cache is enable, and the size of bytes is not greater than {@link DiskCache#maxMemoryCacheSingleSize},
     * it will be cached in memory.
     *
     * @param key key of cache
     * @return bytes of cache
     */
    public byte[] getBytes(String key) {
        CacheItem item = cacheItemMap.get(key);
        if (item == null) {
            return null;
        }
        if (item.getExpiredTime() != null && System.currentTimeMillis() >= item.getExpiredTime()) {
            remove(item);
            return null;
        }
        if (enableMemoryCache) {
            byte[] result = memoryCache.get(key);
            if (result != null) {
                return result;
            }
        }
        byte[] result = readFile(fileOfKey(key));
        if (enableMemoryCache) {
            if (result.length > maxMemoryCacheSingleSize) {
                return result;
            }
            memoryCache.put(key, result);
        }
        return result;
    }

    /**
     * Returns cache as string.
     *
     * @param key key of cache
     * @return string of cache
     */
    public String getString(String key) {
        byte[] bytes = getBytes(key);
        if (bytes == null) return null;
        return new String(bytes, charset);
    }

    /**
     * Returns cache as the specific type.
     *
     * @param key  key of cache
     * @param type type of cached object
     * @param <T>  type of cached object
     * @return object deserialized from bytes
     */
    public <T> T get(String key, Type type) {
        byte[] bytes = getBytes(key);
        if (bytes == null) return null;
        return serializer.decode(bytes, type);
    }

    /**
     * Puts a cache, the object will be serialized.
     *
     * @param key    key of cache
     * @param object the specific object to cache
     */
    public void put(String key, Object object) {
        put(key, serializer.encode(object), CacheOption.empty());
    }

    /**
     * Puts a string cache.
     *
     * @param key     key of cache
     * @param content content to cache
     */
    public void put(String key, String content) {
        putBytes(key, content.getBytes(charset), CacheOption.empty());
    }

    /**
     * Puts bytes to cache.
     *
     * @param key   key of cache
     * @param bytes bytes to cache
     */
    public void put(String key, byte[] bytes) {
        putBytes(key, bytes, CacheOption.empty());
    }

    /**
     * Puts a cache, the object will be serialized, with the specific cache option.
     *
     * @param key    key of cache
     * @param object the specific object to cache
     * @param option option of cache
     */
    public void put(String key, Object object, CacheOption option) {
        put(key, serializer.encode(object), option);
    }

    /**
     * Puts a string cache, with the specific cache option.
     *
     * @param key     key of cache
     * @param content content to cache
     * @param option  option of cache
     */
    public void put(String key, String content, CacheOption option) {
        putBytes(key, content.getBytes(charset), option);
    }

    /**
     * Puts bytes to cache, with the specific cache option.
     *
     * @param key    key of cache
     * @param bytes  bytes to cache
     * @param option option of cache
     */
    public void put(String key, byte[] bytes, CacheOption option) {
        putBytes(key, bytes, option);
    }

    /**
     * Puts bytes to cache, with the specific cache option.
     *
     * @param key    key of cache
     * @param bytes  bytes to cache
     * @param option option of cache
     */
    private void putBytes(String key, byte[] bytes, CacheOption option) {
        if (enableMemoryCache) {
            memoryCache.put(key, bytes);
        }
        CacheItem item = cacheItemMap.get(key);
        if (item == null) {
            item = new CacheItem();
            item.setCreatedTime(System.currentTimeMillis());
            item.setKey(key);
            item.setFilename(newFilename());
            metadata.getItems().add(item);
            cacheItemMap.put(key, item);
        }
        item.setExpiredTime(option.getExpiredTime());
        writeMetadata();
        writeToFile(fileOfKey(key), bytes);
    }

    /**
     * Removes cache from cacheItemMap, metadata, memory cache and disk.
     *
     * @param item cache item to remove
     */
    private void remove(CacheItem item) {
        cacheItemMap.remove(item.getKey());
        metadata.getItems().remove(item);
        if (enableMemoryCache) memoryCache.remove(item.getKey());
        writeMetadata();
        FileHelper.delete(fileOfKey(item.getKey()));
    }

    /**
     * Creates a new File instance of the specific cache key.
     *
     * @param key key of cache
     * @return a new File instance of the specific cache key
     */
    private File fileOfKey(String key) {
        return new File(cachePath, filenameOfKey(key));
    }

    /**
     * Gets the mapped filename of key.
     *
     * @param key key of cache
     * @return filename
     */
    private String filenameOfKey(String key) {
        return cacheItemMap.get(key).getFilename();
    }

    /**
     * Creates a new filename string use UUID.
     *
     * @return a new filename string
     * @see UUID#randomUUID()
     */
    private String newFilename() {
        return UUID.randomUUID().toString();
    }

    /**
     * Writes the metadata to disk.
     */
    private void writeMetadata() {
        String data = new GsonBuilder().setPrettyPrinting().create().toJson(metadata);
        writeToFile(metadataFile, data.getBytes(charset));
    }

    /**
     * Writes bytes to the file.
     *
     * @param file  target
     * @param bytes bytes to write
     */
    private void writeToFile(File file, byte[] bytes) {
        try {
            FileHelper.write(file, bytes);
        } catch (IOException e) {
            e.printStackTrace();
            logger.severe(e.getMessage());
        }
    }

    /**
     * Reads bytes from the file.
     *
     * @param file source
     * @return all bytes of the file, empty array when read failed.
     */
    private byte[] readFile(File file) {
        try {
            return FileHelper.readAll(file);
        } catch (IOException e) {
            e.printStackTrace();
            logger.severe(e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Builder of DiskCache.
     *
     * @see DiskCache
     */
    public static class Builder {
        private File path;
        private String name;
        private Serializer serializer;
        private boolean enableMemoryCache = false;
        private int maxMemoryCacheCount = 5;
        private int maxMemoryCacheSingleSize = 8192;

        public Builder enableMemoryCache() {
            this.enableMemoryCache = true;
            return this;
        }

        public Builder maxMemoryCacheCount(int maxMemoryCacheCount) {
            this.maxMemoryCacheCount = maxMemoryCacheCount;
            return this;
        }

        public Builder maxMemoryCacheSingleSize(int maxMemoryCacheSingleSize) {
            this.maxMemoryCacheSingleSize = maxMemoryCacheSingleSize;
            return this;
        }

        public Builder path(File path) {
            this.path = path;
            return this;
        }

        public Builder path(String path) {
            this.path = new File(path);
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder serializer(Serializer serializer) {
            this.serializer = serializer;
            return this;
        }

        public DiskCache build() {
            return new DiskCache(this);
        }
    }
}
