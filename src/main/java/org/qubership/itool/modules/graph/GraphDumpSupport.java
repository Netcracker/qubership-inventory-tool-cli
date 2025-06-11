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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.modules.report.GraphReportImpl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

/**
 * Support dumping/restoring graph data with all associated stuff.
 */
public class GraphDumpSupport {

    protected static final Logger LOG = LoggerFactory.getLogger(GraphDumpSupport.class);

    public static final int CURRENT_CONTAINER_MODEL_VERSION = 1;

    /** Old graph dumps without any version specified */
    public static final int LEGACY_CONTAINER_MODEL_VERSION = -1;

    /** No graph found */
    public static final int NO_GRAPH_MODEL_VERSION = -2;
    public static final int LEGACY_GRAPH_MODEL_VERSION = -1;

    /** No report found */
    public static final int NO_REPORT_MODEL_VERSION = -2;
    public static final int LEGACY_REPORT_MODEL_VERSION = -1;


    /** Dump graph and associated stuff (error report, fallout report, metainfo, etc...) into JSON.
     *
     * @param graph A graph to dump
     * @param deepCopy Set to false to minimize efforts and footprint; this option may
     * be used if neither dump data nor graph data may be modified directly or indirectly
     * while other one is used.
     * @return A dump
     */
    public static JsonObject dumpToJson(Graph graph, boolean deepCopy) {
        JsonObject dump = new JsonObject();
        // saveModelVersion is currently ignored because we can dump only one version
        dump.put("modelVersion", CURRENT_CONTAINER_MODEL_VERSION);

        dump.put("graph", graph.dumpGraphData(deepCopy));

        GraphReport report = graph.getReport();
        if (report != null) {
            dump.put("report", report.dumpReportData(deepCopy));
        }

        return dump;
    }

    /**
     * Dump graph to SSM-specific format
     * @param graph A graph to dump
     * @param nsMatchRe SSM-specific parameter
     * @param deepCopy Whether deep copy is needed
     * @return SSM-specific dump model
     */
    public static JsonArray dumpToSsmJson(Graph graph, String nsMatchRe, boolean deepCopy) {

        JsonObject graphData = graph.dumpGraphData(deepCopy);
        graphData.remove("modelVersion");

        JsonObject graphDump = new JsonObject();
        graphDump.put("nsMatchRe", nsMatchRe);
        graphDump.put("graph", graphData);

        JsonArray wrapper = new JsonArray();
        wrapper.add(graphDump);
        return wrapper;
    }

    /** Restore dump into existing Graph instance.
     * <b>Objects are copied shallowly!</b>
     *
     * @param target Graph instance to restore the dump into
     * @param dump Dump to restore from
     */
    public static void restoreFromJson(Graph target, JsonObject dump) {

        if (dump == null) {
            throw new NullPointerException("dump is null");
        }

        JsonObject graphDump = dump.getJsonObject("graph");
        if (graphDump == null) {
            throw new NullPointerException("dump.graph is null");
        }
        target.restoreGraphData(graphDump);

        Integer modelVersion = dump.getInteger("modelVersion");

        Object rawReportDump = dump.getValue("report");
        if (rawReportDump != null) {
            GraphReport report = target.getReport();
            if (report == null) {
                report = new GraphReportImpl();
                target.setReport(report);
            }

            JsonObject reportDump;
            if (modelVersion == null && rawReportDump instanceof JsonArray) { // Old files
                reportDump = new JsonObject()
                        .put("modelVersion", GraphReport.CURRENT_REPORT_MODEL_VERSION)
                        .put("records", rawReportDump);
            } else if (modelVersion != null
                    && modelVersion.intValue() == CURRENT_CONTAINER_MODEL_VERSION
                    && rawReportDump instanceof JsonObject)
            {
                reportDump = (JsonObject) rawReportDump;
            } else {
                LOG.error("Invalid or unsupported report format, skipping");
                reportDump = new JsonObject()
                        .put("modelVersion", GraphReport.CURRENT_REPORT_MODEL_VERSION)
                        .put("records", new JsonArray());
            }
            report.restoreReportData(reportDump);
        }
    }

    /** Restore dump into a new Graph instance.
     * <b>Objects are reused and copied shallowly.</b>
     *
     * @param dump Dump to restore Graph and Report from.
     * @return Graph model
     */
    public static Graph restoreFromJson(JsonObject dump) {
        Graph graph = new GraphImpl();
        GraphReport report = new GraphReportImpl();
        graph.setReport(report);
        restoreFromJson(graph, dump);
        return graph;
    }

    /** Get a dump model version of given dump.
     * @param dump A dump
     * @return modelVersion, or {@link #LEGACY_CONTAINER_MODEL_VERSION} for older dumps
     */
    public static int getDumpModelVersion(JsonObject dump) {
        return dump.getInteger("modelVersion", LEGACY_CONTAINER_MODEL_VERSION);
    }

    public static int getGraphModelVersion(JsonObject dump) {
        JsonObject graph = dump.getJsonObject("graph");
        if (graph == null) {
            return NO_GRAPH_MODEL_VERSION;
        }
        return graph.getInteger("modelVersion", LEGACY_GRAPH_MODEL_VERSION);
    }

    public static boolean isGraphDumpSupported(JsonObject dump) {
        int dumpModelVersion = getDumpModelVersion(dump);
        int graphModelVersion = GraphDumpSupport.getGraphModelVersion(dump);

        return (dumpModelVersion == CURRENT_CONTAINER_MODEL_VERSION
               || dumpModelVersion == LEGACY_CONTAINER_MODEL_VERSION)
           && (graphModelVersion <= Graph.CURRENT_GRAPH_MODEL_VERSION);
    }

    public static int getErrorReportModelVersion(JsonObject dump) {
        int containerVersion = getDumpModelVersion(dump);
        switch (containerVersion) {
            case GraphDumpSupport.LEGACY_CONTAINER_MODEL_VERSION: {
                JsonArray report = dump.getJsonArray("report");
                if (report != null) {
                    return LEGACY_REPORT_MODEL_VERSION;
                }
            }
            case GraphDumpSupport.CURRENT_CONTAINER_MODEL_VERSION: {
                JsonObject report = dump.getJsonObject("report");
                if (report != null) {
                    return report.getInteger("modelVersion", LEGACY_REPORT_MODEL_VERSION);
                }
            }
        }
        return NO_REPORT_MODEL_VERSION;
    }

    public static JsonArray getErrorReportRecords(JsonObject dump) {
        int reportVersion = getErrorReportModelVersion(dump);
        switch (reportVersion) {
        case LEGACY_REPORT_MODEL_VERSION:
            return (JsonArray) JsonPointer.from("/report").queryJson(dump);
        case CURRENT_CONTAINER_MODEL_VERSION:
            return (JsonArray) JsonPointer.from("/report/records").queryJson(dump);
        case NO_REPORT_MODEL_VERSION:
            LOG.error("No error report");
            return null;
        }
        LOG.error("Unrecognized dump version");
        return null;
    }

}
