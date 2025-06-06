/*
 * Copyright 2024-2025 NetCracker Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qubership.itool.utils;

import io.vertx.core.json.JsonObject;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static org.qubership.itool.modules.graph.Graph.F_DIRECTORY;


/** File system and other IO */
public class FSUtils {

    public static final int DEFAULT_BUFFER_SIZE = 8192;
    private static YamlParser parser = new YamlParser();

    public static String getComponentDirPath(JsonObject component) {
        return component.getString(F_DIRECTORY);
    }

    public static String relativePath(JsonObject component, String fileName) {
        Path absPath = Path.of(fileName).toAbsolutePath();
        Path compPath = Path.of(component.getString(F_DIRECTORY)).toAbsolutePath();
        String relativePath = compPath.relativize(absPath).toString().replaceAll("\\\\", "/");
        return relativePath;
    }

    public static String getConfigFilePath(JsonObject config, String... path) {
        Path configFilePath = ConfigUtils.getConfigFilePath(config, path);
        return configFilePath.toString();
    }

    public static void appendFile(String file, String text) throws IOException {
        Files.writeString(Path.of(file), text, CREATE, APPEND);
    }

    public static void createFile(String file, String text) throws IOException {
        Files.writeString(Path.of(file), text, CREATE);
    }

    public static String readFileAsIs(String path) throws IOException {
        return Files.readString(Paths.get(path));
    }

    public static String readFileSafe(String path) throws IOException {
        return readFileAsIs(path).replaceAll("[^\\n\\r\\t\\p{Print}]", "?");
    }

    public static String getFolder(String fileLocation) {
        return Path.of(fileLocation).getParent().toString();
    }

    /* Open raw stream from URL. May return {@code null} for missing resources. */
    public static InputStream openRawUrlStream(Class<?> caller, String location) throws IOException {
        if (location.startsWith("classpath:")) {
            // May just return null
            return caller.getResourceAsStream(location.substring("classpath:".length()));
        }
        return new URL(location).openStream();  // Can it return null or always throws an IOException?
    }

    public static synchronized Object getYamlFileContents(Class<?> caller, String location) throws IOException {
        try (Reader reader = new InputStreamReader(
                FSUtils.openUrlStream(caller, location), JsonUtils.UTF_8))
        {
            List<Object> data = parser.parseYaml(reader, "yaml-parser/spring.yml");
            parser.fixSpringYamlModel(data);

            return data;
        }
    }

    /*** Open a stream from URL with buffering and GZip support.
     *
     * @param caller Caller class. Used for "classpath:" locations.
     * @param location Location, usually something like this: "file:...", "classpath:...".
     * If it ends with ".gz", content is un-gzipped.
     * @return The input stream
     * @throws IOException IO happened
     */
    public static InputStream openUrlStream(Class<?> caller, String location) throws IOException {
        InputStream is = openRawUrlStream(caller, location);
        if (is == null) {
            return null;
        } else if (location.endsWith(".gz")) {
            return new GZIPInputStream(is, DEFAULT_BUFFER_SIZE);
        } else if (is instanceof BufferedInputStream) {
            return is;
        } else {
            return new BufferedInputStream(is, DEFAULT_BUFFER_SIZE);
        }
    }

    /*** Create a stream to write into a file with buffering and GZip support.
     *
     * @param path Target path. If it ends with ".gz", content is gzipped.
     * @param options Open options
     * @return The output stream
     * @throws IOException IO happened
     */
    public static OutputStream createFileOutputStream(Path path, OpenOption... options) throws IOException {
        String fileName = path.getFileName().toString();
        OutputStream fos = null;
        try {
            fos = Files.newOutputStream(path, options);
            if (fileName.endsWith(".gz")) {
                return new GZIPOutputStream(fos, DEFAULT_BUFFER_SIZE);
            } else {
                return new BufferedOutputStream(fos, DEFAULT_BUFFER_SIZE);
            }
        } catch (IOException e) {
            if (fos != null) {
                fos.close();
            }
            throw e;
        }
    }

}
