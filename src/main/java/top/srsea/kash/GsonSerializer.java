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

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * A serializer based on {@link Gson}.
 *
 * @author sea
 * @see Serializer
 */
public class GsonSerializer implements Serializer {

    /**
     * Charset to convert between string and bytes.
     */
    private Charset charset = StandardCharsets.UTF_8;

    /**
     * Deserialize an object from bytes.
     *
     * @param bytes bytes to decode
     * @param type  type of object
     * @param <T>   class of object
     * @return object decoded
     */
    @Override
    public <T> T decode(byte[] bytes, Type type) {
        return new Gson().fromJson(new String(bytes, charset), type);
    }

    /**
     * Serialize an object to bytes.
     *
     * @param object object to encode
     * @return bytes encoded
     */
    @Override
    public byte[] encode(Object object) {
        return new Gson().toJson(object).getBytes(charset);
    }
}
