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

package org.qubership.itool.modules.report;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface GraphReport {

    int CURRENT_REPORT_MODEL_VERSION = 1;

    //--- Elements of entries (JsonObject's) returned by dumpRecords()
    String TYPE = "type";
    String COMPONENT = "component";
    String MESSAGE = "message";
    String GRAPH_ID = "graphId";

    //--- Report types
    String CONF_ERROR = "CONF_ERROR";
    String EXCEPTION = "EXCEPTION";
    String ERROR = "ERROR";
    String MERGE_ERROR = "MERGE_ERROR";


    //--- Manage entire report

    /**
     * Create report dump for saving. Format is implementation-specific.
     *
     * @param deepCopy Whether deep copy is needed to ensure data safety
     * @return Piece of dump containing report data
     */
    default JsonObject dumpReportData(boolean deepCopy) {
        return dumpReportData(CURRENT_REPORT_MODEL_VERSION, deepCopy);
    }

    public JsonObject dumpReportData(int modelVersion, boolean deepCopy);

    /**
     * Extract report entries.
     *
     * @param deepCopy Whether deep copy is needed to ensure data safety
     * @return Report entries
     */
    public JsonArray dumpRecords(boolean deepCopy);

    public void clear();

    void restoreReportData(JsonObject reportData);

    /* @deprecated Effectively, used only by unit tests */
    void restoreRecords(JsonArray reportRecords);

    //--- Add entries

    public void addRecord(JsonObject record);

    void addMessage(String type, JsonObject sourceComponent, String message);

    void mandatoryValueMissed(JsonObject sourceComponent, String property);

    void referenceNotFound(JsonObject sourceComponent, String reference);

    void configurationFileNotFound(JsonObject sourceComponent, String configurationFile);

    void componentDuplicated(JsonObject sourceComponent, JsonObject duplicatedComponent);

    void conventionNotMatched(JsonObject sourceComponent, String pattern, String value);

    void exceptionThrown(JsonObject sourceComponent, Exception exception);

    void mergingError(JsonObject sourceDesc, String message);

    void mergingError(JsonObject sourceDesc, Exception exception);

    void internalError(String message);

}
