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

package org.qubership.itool.modules.graph;

import io.vertx.core.json.JsonObject;
import org.qubership.itool.modules.report.GraphReport;

import java.util.List;

public interface Graph extends BasicGraph {

    int CURRENT_GRAPH_MODEL_VERSION = 3;
    int FIRST_VERSIONED_GRAPH_MODEL_VERSION = 1;

    //--- Common attributes
    String F_ID = "id";
    String F_TYPE = "type";
    String F_NAME = "name";
    String F_VERSION = "version";
    String F_REPOSITORY = "repository";
    String F_DIRECTORY = "directoryPath";
    String F_PROCESSING_ISSUES = "processingIssues";

    /**
     * <p>This attribute is set to true to indicate mock vertices that need to be replaced with
     * non-mock vertices <b>identified by id</b> by merging with another graph that contains needed data.
     * <p>Value type: boolean.
     */
    String F_MOCK_FLAG = "isMock";

    String F_MICROSERVICE_FLAG = "isMicroservice";

    /**
     * <p>This attribute is used to indicate mock vertices that need to be replaced with
     * non-mock vertices <b>identified by given list of attributes</b> by merging with
     * another graph that contains needed data.
     * <p>Value type: List of JSON pathes (strings)
     */
    String F_MOCKED_FOR = "mockedFor";

    String F_DNS_NAME = "dnsName";
    String F_DNS_NAMES = "dnsNames";
    String P_DETAILS_DNS_NAME = "/details/dnsName";
    String P_DETAILS_DNS_NAMES = "/details/dnsNames";
    String P_DETAILS_LANGUAGE = "/details/language";
    String F_DETAILS = "details";

    //--- Common vertex types
    String V_UNKNOWN = "unknown";
    String V_ROOT = "root";
    String V_DOMAIN = "domain";
    String V_APPLICATION = "application";
    String V_MICROSERVICE = "microservice";
    String V_LIBRARY = "library";
    String V_BACKEND = "backend";
    String V_ERROR_CODE = "errorCode";

    //--- Graph operations

    List<JsonObject> getRootSuccessors();

    boolean addVertex(JsonObject vertex);

    boolean addVertex(String sourceVertexId, JsonObject destinationVertex);

    boolean addVertex(JsonObject sourceVertex, JsonObject destinationVertex);

    boolean addVertexUnderRoot(JsonObject vertex);

    /** Change vertex id, update edges accordingly.
     *
     * @param vertex A vertex to relocate
     * @param newId New vertex id
     * @return Success indicator
     */
    boolean relocateVertex(JsonObject vertex, String newId);

    boolean removeVertex(JsonObject vertex);

    /** Add an edge. Add its ends (vertices) to this graph if needed.
     *
     * @param sourceVertex Source vertex for the edge being added
     * @param destinationVertex Destination vertex for the edge being added
     * @return id of added edge, or {@code null} if nothing was added because similar edge
     *     already exists, and duplicates are not allowed.
     */
    String addEdge(JsonObject sourceVertex, JsonObject destinationVertex);

    /** Add an edge. Add its ends (vertices) to this graph if needed.
     *
     * @param sourceVertex Source vertex for the edge being added
     * @param destinationVertex Destination vertex for the edge being added
     * @param edge Edge JSON
     * @return id of added edge, or {@code null} if nothing was added because similar edge
     *     already exists, and duplicates are not allowed.
     */
    String addEdge(JsonObject sourceVertex, JsonObject destinationVertex, JsonObject edge);

    int removeAllEdges(JsonObject sourceVertex, JsonObject destinationVertex);

    void clear();

    //--- Dump/restore and manage

    /** Get JSON dump of graph data <b>only</b> (including vertices and edges,
     * but excluding associated report or anything else).
     *
     * @param deepCopy Whether deep copy is required. Use {@code false} if this graph can be
     *     disposed right after creating a dump.
     * @return Dump of graph data.
     */

    JsonObject dumpGraphData(boolean deepCopy);

    /** Restore graph data <b>only</b> from dump. Objects are copied shallowly.
     *
     * @param dump Graph data dump.
     */
    void restoreGraphData(JsonObject dump);

    void printGraph();

    //--- Associated report

    GraphReport getReport();

    void setReport(GraphReport report);

    int getGraphVersion();

    void setGraphVersion(int graphVersion);
}
