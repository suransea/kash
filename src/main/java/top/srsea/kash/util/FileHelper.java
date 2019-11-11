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

import java.io.*;
import java.util.Arrays;

/**
 * Utilities of file operation.
 *
 * @author sea
 */
public class FileHelper {

    /**
     * The maximum size of array to allocate.
     */
    private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;
    /**
     * Buffer size used for reading and writing.
     */
    private static final int BUFFER_SIZE = 8192;

    private FileHelper() {
    }

    /**
     * Deletes all the files under the specific path.
     *
     * @param file        file path
     * @param reserveSelf if reserve the file path self
     */
    private static void delete(File file, boolean reserveSelf) {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    delete(child);
                }
            }
        }
        if (!reserveSelf) {
            boolean deleted = file.delete(); //ignore if delete successfully
        }
    }

    /**
     * Deletes the specific file or directory.
     *
     * @param file the specific file or directory
     */
    public static void delete(File file) {
        delete(file, false);
    }

    /**
     * Deletes all the files under the specific directory, if it is a directory.
     *
     * @param file the specific directory
     */
    public static void deleteUnder(File file) {
        delete(file, true);
    }

    /**
     * Reads all bytes from the specific file.
     *
     * @param file the specific file
     * @return bytes of file
     * @throws IOException file is too large, or IO errors occurred during reading
     */
    public static byte[] readAll(File file) throws IOException {
        int length = (int) file.length();
        if (length > MAX_BUFFER_SIZE) {
            throw new OutOfMemoryError("Required array size too large");
        }
        InputStream source = new FileInputStream(file);
        byte[] bytes = read(source, length);
        source.close();
        return bytes;
    }

    /**
     * Writes all bytes to the specific file, file will be overwritten.
     *
     * @param file  the specific file
     * @param bytes bytes to write
     * @throws IOException if IO errors occurred during writing
     */
    public static void write(File file, byte[] bytes) throws IOException {
        OutputStream target = new FileOutputStream(file);
        int len = bytes.length;
        int rem = len;
        while (rem > 0) {
            int n = Math.min(rem, BUFFER_SIZE);
            target.write(bytes, (len - rem), n);
            rem -= n;
        }
        target.close();
    }

    /**
     * Reads all bytes from the input stream.
     *
     * @param source      input stream
     * @param initialSize initial buffer capacity
     * @return bytes read
     * @throws IOException source has too many bytes, or IO errors occurred during reading
     */
    private static byte[] read(InputStream source, int initialSize) throws IOException {
        int capacity = initialSize;
        byte[] buf = new byte[capacity];
        int nread = 0;
        int n;
        for (; ; ) {
            // read to EOF, which may read more or less than initialSize (eg: file
            // is truncated while we are reading)
            while ((n = source.read(buf, nread, capacity - nread)) > 0)
                nread += n;

            // if last call to source.read() returned -1, we are done
            // otherwise, try to read one more byte; if that failed we're done too
            if (n < 0 || (n = source.read()) < 0)
                break;

            // one more byte was read; need to allocate a larger buffer
            if (capacity <= MAX_BUFFER_SIZE - capacity) {
                capacity = Math.max(capacity << 1, BUFFER_SIZE);
            } else {
                if (capacity == MAX_BUFFER_SIZE)
                    throw new OutOfMemoryError("Required array size too large");
                capacity = MAX_BUFFER_SIZE;
            }
            buf = Arrays.copyOf(buf, capacity);
            buf[nread++] = (byte) n;
        }
        return (capacity == nread) ? buf : Arrays.copyOf(buf, nread);
    }
}
