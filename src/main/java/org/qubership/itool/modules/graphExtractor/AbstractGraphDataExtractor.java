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

package org.qubership.itool.modules.graphExtractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graph.GraphDumpSupport;
import org.qubership.itool.utils.JsonArrayDeserializer;
import org.qubership.itool.utils.JsonObjectDeserializer;
import org.qubership.itool.utils.JsonUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;

public abstract class AbstractGraphDataExtractor implements GraphDataExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGraphDataExtractor.class);

    private static final ObjectMapper mapper;
    static {
        mapper = DatabindCodec.mapper().copy();

        SimpleModule module = new SimpleModule();
        module.addDeserializer(JsonObject.class, new JsonObjectDeserializer());
        module.addDeserializer(JsonArray.class, new JsonArrayDeserializer());
        mapper.registerModule(module);
    }

    @Override
    abstract public JsonObject getDataFromGraph(Graph graph);

    @Override
    public JsonObject extractGraphData(InputStream inputStream, Charset charset) {
        if (inputStream == null) {  // This happens if source was not found
            return null;
        }
        Reader reader = new InputStreamReader(inputStream, charset);
        JsonObject graphDump = null;
        try {
            graphDump = mapper.readValue(reader, JsonObject.class);
        } catch (IOException e) {
            getLogger().error("Failed to load graph from input stream: {}", ExceptionUtils.getStackTrace(e));
        }

        Graph graph = GraphDumpSupport.restoreFromJson(graphDump);
        return getDataFromGraph(graph);
    }

    @Override
    public JsonObject extractGraphData(Path graphFile) {
        if (!graphFile.toFile().exists()) {
            return new JsonObject();
        }
        Graph graph = null;
        try {
            JsonObject dumpFile = JsonUtils.readJsonFile(graphFile.toAbsolutePath().toString());
            graph = GraphDumpSupport.restoreFromJson(dumpFile);
        } catch (Exception e) {
            getLogger().error("Failed to load graph resource using path {}: {}", graphFile, ExceptionUtils.getStackTrace(e));
        }
        return getDataFromGraph(graph);
    }

    protected Logger getLogger() {
        return LOGGER;
    }
}
