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

package top.srsea.kash.pojo;

import java.util.List;

/**
 * A pojo of cache metadata.
 *
 * @author sea
 */
public class Metadata {

    /**
     * Cache name.
     */
    private String name;

    /**
     * Items of this cache.
     *
     * @see CacheItem
     */
    private List<CacheItem> items;

    //getter and setter begin

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<CacheItem> getItems() {
        return items;
    }

    public void setItems(List<CacheItem> items) {
        this.items = items;
    }

    //getter and setter end
}
