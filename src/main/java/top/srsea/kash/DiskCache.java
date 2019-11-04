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

public class DiskCache {
    private static final String FILENAME_METADATA = "kash-metadata.json";
    private static final String DEFAULT_CACHE_NAME = "kash";
    private static Logger logger = Logger.getLogger("DiskCache");
    private Charset charset = StandardCharsets.UTF_8;
    private Metadata metadata;
    private String name;
    private File cachePath;
    private File metadataFile;
    private Map<String, CacheItem> cacheItemMap;
    private Serializer serializer;
    private boolean enableMemoryCache;
    private int maxMemoryCacheCount;
    private int maxMemoryCacheSingleSize;
    private LruCache<String, byte[]> memoryCache;

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
            flushMetadata();
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
        flushMetadata();
        if (enableMemoryCache) {
            memoryCache = new LruCache<>(maxMemoryCacheCount);
        }
        logger.info(String.format("DiskCache %s initialized.", name));
    }

    public void evictAll() {
        metadata.getItems().clear();
        cacheItemMap.clear();
        if (enableMemoryCache) memoryCache.evictAll();
        FileHelper.deleteUnder(cachePath);
    }

    public void remove(String key) {
        CacheItem item = cacheItemMap.get(key);
        if (item == null) return;
        remove(item);
    }

    public int size() {
        return metadata.getItems().size();
    }

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

    public String getString(String key) {
        byte[] bytes = getBytes(key);
        if (bytes == null) return null;
        return new String(bytes, charset);
    }

    public <T> T get(String key, Type type) {
        byte[] bytes = getBytes(key);
        if (bytes == null) return null;
        return serializer.decode(bytes, type);
    }

    public void put(String key, Object object) {
        put(key, serializer.encode(object), CacheOption.empty());
    }

    public void put(String key, String content) {
        putBytes(key, content.getBytes(charset), CacheOption.empty());
    }

    public void put(String key, byte[] bytes) {
        putBytes(key, bytes, CacheOption.empty());
    }

    public void put(String key, Object object, CacheOption option) {
        put(key, serializer.encode(object), option);
    }

    public void put(String key, String content, CacheOption option) {
        putBytes(key, content.getBytes(charset), option);
    }

    public void put(String key, byte[] bytes, CacheOption option) {
        putBytes(key, bytes, option);
    }

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
        flushMetadata();
        writeToFile(fileOfKey(key), bytes);
    }

    private void remove(CacheItem item) {
        cacheItemMap.remove(item.getKey());
        metadata.getItems().remove(item);
        if (enableMemoryCache) memoryCache.remove(item.getKey());
        flushMetadata();
        FileHelper.delete(fileOfKey(item.getKey()));
    }

    private File fileOfKey(String key) {
        return new File(cachePath, filenameOfKey(key));
    }

    private String filenameOfKey(String key) {
        return cacheItemMap.get(key).getFilename();
    }

    private String newFilename() {
        return UUID.randomUUID().toString();
    }

    private void flushMetadata() {
        String data = new GsonBuilder().setPrettyPrinting().create().toJson(metadata);
        writeToFile(metadataFile, data.getBytes(charset));
    }

    private void writeToFile(File file, byte[] bytes) {
        try {
            FileHelper.write(file, bytes);
        } catch (IOException e) {
            e.printStackTrace();
            logger.severe(e.getMessage());
        }
    }

    private byte[] readFile(File file) {
        try {
            return FileHelper.readAll(file);
        } catch (IOException e) {
            e.printStackTrace();
            logger.severe(e.getMessage());
            return new byte[0];
        }
    }

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
