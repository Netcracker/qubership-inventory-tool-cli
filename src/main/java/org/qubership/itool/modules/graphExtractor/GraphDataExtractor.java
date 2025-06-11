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

import org.qubership.itool.modules.graph.Graph;
import io.vertx.core.json.JsonObject;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;

/**
 * Extractor to be used in order to retrieve various data from provided graph.json file per component
 */
public interface GraphDataExtractor {

    /**
     * Extracts the data from graph using inputStream
     *
     * Key of the outer map is a component name
     * Inner map keys contain the name of the gathered data aspect (i.e. "framework", or "language") with values
     * representing the lists of retrieved values
     * @param inputStream inputStream with Graph
     * @param charset {@link java.nio.charset.Charset}
     * @return Json structure containing the components names with respective sets of data provided by interface implementation
     */
    JsonObject extractGraphData(InputStream inputStream, Charset charset);

    /**
     * Extracts the data from graph file stored in given path
     *
     * Key of the outer map is a component name
     * Inner map keys contain the name of the gathered data aspect (i.e. "framework", or "language") with values
     * representing the lists of retrieved values
     * @param graphFile path to file with graph
     * @return Json structure containing the components names with respective sets of data provided by interface implementation
     */
    JsonObject extractGraphData(Path graphFile);

    /**
     * Extracts data from provided Graph
     * @param graph data source in a form of a Graph object
     * @return Json structure containing the components names with respective sets of data provided by interface implementation
     */
    JsonObject getDataFromGraph(Graph graph);
}
