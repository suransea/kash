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

/**
 * Cache options when put an item.
 *
 * @author sea
 */
public class CacheOption {

    /**
     * Expired time of cache item.
     */
    private Long expiredTime;

    /**
     * Creates a new CacheOption contains the specific expired time.
     *
     * @param expiredTime the specific expired time
     * @return CacheOption contains the specific expired time
     */
    public static CacheOption expiredTime(long expiredTime) {
        CacheOption option = new CacheOption();
        option.expiredTime = expiredTime;
        return option;
    }

    /**
     * Creates a new CacheOption contains no option.
     *
     * @return CacheOption contains no option
     */
    public static CacheOption empty() {
        return new CacheOption();
    }

    //getter and setter begin

    public Long getExpiredTime() {
        return expiredTime;
    }

    public void setExpiredTime(Long expiredTime) {
        this.expiredTime = expiredTime;
    }

    //getter and setter end
}
