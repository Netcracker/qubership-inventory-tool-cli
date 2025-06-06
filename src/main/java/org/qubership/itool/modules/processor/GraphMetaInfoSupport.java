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

package org.qubership.itool.modules.processor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import org.qubership.itool.modules.graph.FalloutDto;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graph.GraphDataConstants;
import org.qubership.itool.modules.graph.GraphDumpSupport;
import org.qubership.itool.utils.ConfigUtils;
import org.qubership.itool.utils.JsonUtils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

import static org.qubership.itool.utils.JsonUtils.*;


/**
 * Encapsulates all operations with format of meta-info.
 *
 * <b>Note that descriptions provided to {@link MergerApi} has format different from
 * format stored in graph dumps.</b>
 * In source code, JsonObject in format accepted by MergerApi is usually referred
 * to as "desc", and meta-info as it is stored in graph is usually referred to as "info" or "meta-info".
 *
 * <p>Current meta-info structure: the following attributes are added to (and then read from)
 * {@code "root"} nodes of generated graphs:<pre>
 * root
 *  \ meta: {  -- Description what this graph is
 *      type: "component"|"application"|"namespace"|"unknown",
 *      name: $name, version: $version
 *    },
 *  \ assembly  -- Description of assembly sources
 *    \ sourceGraphs: [  -- For input graphs with identified origin.
 *        { type: $type, name: $name, version: $version, fileName: $fileName,
 *          assembly: { ... recursive structure for assembled sources, including "sourcesDropped" ... }
 *        },
 *        ...
 *      ],
 *    \ sourcesDropped: [  -- When some of input graphs were invalid
 *        { reason: $reason,
 *          type: $type, name: $name, version: $version, fileName: $name,
 *          reportOrigin: { fileName, type, name, version }  -- in case of cumulative report, flat structure
 *        }, ...
 *      ]
 * </pre>
 */
public class GraphMetaInfoSupport {

    public static final String UNKNOWN = GraphDataConstants.UNKNOWN;

    public static final String META = "meta";
    public static final String ASSEMBLY = "assembly";
    public static final String SOURCES = "sourceGraphs";
    public static final String SOURCES_DROPPED = "sourcesDropped";
    public static final String DROPPED_REPORT_ORIGIN = "reportOrigin";

    public static final String TYPE = "type";
    public static final String NAME = "name";
    public static final String FILE_NAME = MergerApi.P_FILE_NAME;
    public static final String VERSION = "version";
    public static final String INVENTORY_TOOL_VERSION = "aditVersion";

    public static final String TYPE_APPLICATION = "application";
    public static final String TYPE_COMPONENT = "component";
    public static final String TYPE_NAMESPACE = "namespace";


    public static final JsonPointer srcGraphsPtr = JsonPointer.from("/" + ASSEMBLY + "/" + SOURCES);
    public static final JsonPointer srcGraphsDroppedPtr = JsonPointer.from("/" + ASSEMBLY + "/" + SOURCES_DROPPED);


    //------------------------------------------------------
    // Fallout reports

    /** Get fallout report from dump.
     * @see JsonUtils#getFalloutReportFromDump(JsonObject)
     *
     * @param dump Dump to extract fallout report from
     * @return Fallout report
     */
    public static List<FalloutDto> getFalloutReportFromDump(JsonObject dump) {
        if (! GraphDumpSupport.isGraphDumpSupported(dump)) {
            throw new IllegalArgumentException("Graph dump version is not supported: " + GraphDumpSupport.getGraphModelVersion(dump));
        }
        JsonArray array = (JsonArray) JsonPointer.from("/graph/root/" + ASSEMBLY + "/" + SOURCES_DROPPED).queryJson(dump);
        if (array == null) {
            return Collections.emptyList();
        }

        JsonObject meta = (JsonObject) JsonPointer.from("/graph/root/" + META).queryJson(dump);

        List<FalloutDto> list = array.stream().map(item -> getFalloutItem(item, meta)).collect(Collectors.toList());
        return list;
    }

    public static FalloutDto getFalloutItem(Object item, JsonObject meta) {
        JsonObject src = JsonUtils.asJsonObject(item);
        String appName, appVersion, msName;
        switch (src.getString(Graph.F_TYPE, UNKNOWN)) {
        case TYPE_COMPONENT:
            // Try to get app name from reportOrigin
            Object o = JsonPointer.from("/" + DROPPED_REPORT_ORIGIN + "/" + NAME).queryJson(src);
            if (o == null && meta != null && TYPE_APPLICATION.equals(meta.getValue(TYPE))) {    // Try to get app name from meta
                o = meta.getValue(NAME);
            }
            appName = o != null ? o.toString() : UNKNOWN;

            // Try to get app version from reportOrigin
            o = JsonPointer.from("/" + DROPPED_REPORT_ORIGIN + "/" + VERSION).queryJson(src);
            if (o == null && meta != null && TYPE_APPLICATION.equals(meta.getValue(TYPE))) {    // Try to get app version from meta
                o = meta.getValue(VERSION);
            }
            appVersion = o != null ? o.toString() : UNKNOWN;

            msName = src.getString(NAME, UNKNOWN);
            break;

        case TYPE_APPLICATION:
            appName = src.getString(NAME, UNKNOWN);
            appVersion = src.getString(VERSION, UNKNOWN);
            msName = UNKNOWN;
            break;

        default:    // namespace or something strange
            appName = src.getString(NAME, UNKNOWN);
            appVersion = src.getString(VERSION, UNKNOWN);
            msName = UNKNOWN;
            break;
        }
        return new FalloutDto(appName, appVersion, msName);
    }


    //------------------------------------------------------
    // Create meta-info

    public static String getInventoryToolVersion() {
        return (String)ConfigUtils.getInventoryToolBuildProperties().get("inventory.tool.version");
    }

    /** Create meta-info inside a component graph.
     *
     * @param graph Graph model
     * @param compName Component name
     * @param compVersion Component version
     * @return Meta-info model
     */
    public static JsonObject initMetaInfoForComponent(Graph graph, String compName, String compVersion) {
        JsonObject meta = getMetaInfoForComponent(compName, compVersion);
        graph.getVertex(Graph.V_ROOT).put(META, meta);
        return meta;
    }

    public static JsonObject getMetaInfoForComponent(String compName, String compVersion) {
        if (StringUtils.isEmpty(compVersion)) {
            compVersion = UNKNOWN;
        }
        JsonObject meta = new JsonObject()
                .put(TYPE, TYPE_COMPONENT)
                .put(NAME, compName)
                .put(VERSION, compVersion)
                .put(INVENTORY_TOOL_VERSION, getInventoryToolVersion())
                ;
        return meta;
    }

    /** Create meta-info inside an application graph.
     *
     * @param graph Graph model
     * @param appName Application name
     * @param appVersion Application version
     * @return Meta-info model
     */
    public static final JsonObject initMetaInfoForApplication(Graph graph, String appName, String appVersion) {
        JsonObject meta = getMetaInfoForApplication(appName, appVersion);
        graph.getVertex(Graph.V_ROOT).put(META, meta);
        return meta;
    }

    public static final JsonObject getMetaInfoForApplication(String appName, String appVersion) {
        if (StringUtils.isEmpty(appVersion)) {
            appVersion = UNKNOWN;
        }
        JsonObject meta = new JsonObject()
            .put(TYPE, TYPE_APPLICATION)
            .put(NAME, appName)
            .put(VERSION, appVersion)
            .put(INVENTORY_TOOL_VERSION, getInventoryToolVersion())
            ;
        return meta;
    }


    //------------------------------------------------------
    // Support for GraphMerger

    /**
     * Fill meta-information inside graph model from its provenance descriptor.
     * Provided descriptor has precedence over meta-info already present inside graph.
     *
     * <p>Recognized attributes from provenance descriptor:
     * <ul><li>P_IS_APPLICATION: Boolean
     * <li>P_APP_NAME: String
     * <li>P_APP_VERSION: String
     * <li>P_IS_NAMESPACE: Boolean
     * <li>P_NAMESPACE_NAME: String
     * </ul>
     *
     * <p>Example transformation for target graph with no pre-existing meta-info:
     * <code>{ P_IS_APPLICATION: true, P_APP_NAME: "MyApp", P_APP_VERSION: "main-SNAPSHOT" } -&gt;
     * { "type": "application", "name": "MyApp", "version": "main-SNAPSHOT", "aditVersion": "2.5.1-SNAPSHOT" }
     * </code>
     *
     * @param graph Graph model (target)
     * @param graphDesc Descriptor of graph provenance (source)
     */
    public static void initMetaInfoFromDesc(Graph graph, JsonObject graphDesc) {
        enrichGraphDesc(graph, graphDesc);

        JsonObject targetRoot = graph.getVertex(Graph.V_ROOT);

        JsonObject meta = getOrCreateJsonObject(targetRoot, META);
        copyValueIfNotNull(graphDesc, meta, TYPE);
        copyValueIfNotNull(graphDesc, meta, NAME);
        copyValueIfNotNull(graphDesc, meta, VERSION);
        meta.put(INVENTORY_TOOL_VERSION, getInventoryToolVersion());

        JsonObject assembly = getOrCreateJsonObject(targetRoot, ASSEMBLY);
        getOrCreateJsonArray(assembly, SOURCES);
        getOrCreateJsonArray(assembly, SOURCES_DROPPED);
    }

    /**
     * Enrich graph descriptor with data from graph meta-info, if any is present.
     * Fill absent attributes in descriptor of they are present in meta-info.
     * Provided descriptor has precedence over meta-info inside graph.
     *
     * @param graph Graph model (source)
     * @param graphDesc Descriptor of graph provenance (target)
     */
    public static void enrichGraphDesc(Graph graph, JsonObject graphDesc) {
        JsonObject graphMeta = graph.getVertex(Graph.V_ROOT).getJsonObject(META);
        copyValueIfAbsent(graphMeta, graphDesc, TYPE);
        copyValueIfAbsent(graphMeta, graphDesc, NAME);
        copyValueIfAbsent(graphMeta, graphDesc, VERSION);
        copyValueIfAbsent(graphMeta, graphDesc, INVENTORY_TOOL_VERSION);
        if (graphDesc.getString(TYPE) == null) {
            if (graphDesc.getString(MergerApi.P_FILE_NAME) != null) {
                graphDesc.put(TYPE, TYPE_COMPONENT);
            } else {
                graphDesc.put(TYPE, UNKNOWN);
            }
        }

        if (graphDesc.getBoolean(MergerApi.P_IS_APPLICATION, false) == true) {
            graphDesc.put(TYPE, TYPE_APPLICATION)
                    .put(NAME, graphDesc.getString(MergerApi.P_APP_NAME));
            String appVersion = graphDesc.getString(MergerApi.P_APP_VERSION);
            if (StringUtils.isEmpty(appVersion)) {
                appVersion = graphDesc.getString(VERSION);
                if (StringUtils.isEmpty(appVersion)) {
                    appVersion = UNKNOWN;
                }
            }
            graphDesc.put(VERSION, appVersion);
        } else if (graphDesc.getBoolean(MergerApi.P_IS_NAMESPACE, false) == true) {
            graphDesc.put(TYPE, TYPE_NAMESPACE)
                    .put(NAME, graphDesc.getString(MergerApi.P_NAMESPACE_NAME));
        }
    }

    public static void addDroppedItem(InvalidGraphException e, JsonObject sourceDesc, Graph targetGraph) {
        JsonObject errorReport = new JsonObject();
        errorReport.put("reason", e.getReason());
        copyValueIfNotNull(sourceDesc, errorReport, FILE_NAME);
        copyValueIfNotNull(sourceDesc, errorReport, TYPE);
        copyValueIfNotNull(sourceDesc, errorReport, NAME);
        copyValueIfNotNull(sourceDesc, errorReport, VERSION);
        copyValueIfNotNull(sourceDesc, errorReport, INVENTORY_TOOL_VERSION);
        copyValueIfNotNull(sourceDesc, errorReport, ASSEMBLY);

        JsonObject targetRoot = targetGraph.getVertex(Graph.V_ROOT);
        ((JsonArray)srcGraphsDroppedPtr.queryJson(targetRoot)).add(errorReport);
    }

    // Accepts descriptors (see constants in MergerApi), updates meta-info
    public static void mergeAssemblyInfo(JsonObject sourceRoot, JsonObject sourceDesc,
            JsonObject targetRoot, JsonObject targetDesc) {

        // desc has been merged from source graph meta and provided desc
        // Generate copy of source info with copy of "assembly" attribute...
        JsonObject sourceMetaInfo = copyDescToInfo(sourceDesc);
        JsonObject sourceRootMeta = sourceRoot.getJsonObject(ASSEMBLY);
        if (sourceRootMeta != null) {
            sourceMetaInfo.put(ASSEMBLY, sourceRootMeta.copy());
        }
        copyValueIfAbsent(sourceRootMeta, targetDesc, INVENTORY_TOOL_VERSION);

        // ... add that copy of source meta-info into "/assembly/sourceGraphs" of target meta-info
        JsonArray sourcesList = JsonUtils.getOrCreateJsonArray(targetRoot, srcGraphsPtr);
        sourcesList.add(sourceMetaInfo);

        // Propagate fallout report from source graph to target fallout report
        JsonArray sourceFallout = sourceRootMeta != null ? sourceRootMeta.getJsonArray(SOURCES_DROPPED) : null;
        if (sourceFallout != null) {
            for (Object obj: sourceFallout) {
                JsonObject jsonFallout = asJsonObject(obj);
                JsonObject errorReport = jsonFallout.copy();
                errorReport.put(DROPPED_REPORT_ORIGIN, copyDescToInfo(sourceDesc));
                ((JsonArray)srcGraphsDroppedPtr.queryJson(targetRoot)).add(errorReport);
            }
        }
    }

    public static JsonObject copyDescToInfo(JsonObject graphDesc) {
        JsonObject metaInfo = new JsonObject();
        copyValueIfNotNull(graphDesc, metaInfo, TYPE);
        copyValueIfNotNull(graphDesc, metaInfo, NAME);
        copyValueIfNotNull(graphDesc, metaInfo, VERSION);
        copyValueIfNotNull(graphDesc, metaInfo, FILE_NAME);
        copyValueIfNotNull(graphDesc, metaInfo, INVENTORY_TOOL_VERSION);
        return metaInfo;
    }

}
