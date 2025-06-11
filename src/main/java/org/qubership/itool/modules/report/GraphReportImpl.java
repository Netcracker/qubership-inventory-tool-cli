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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.modules.graph.GraphDataConstants;
import org.qubership.itool.modules.processor.InvalidGraphException;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.qubership.itool.modules.graph.Graph.F_ID;

public class GraphReportImpl implements GraphReport {

    protected static final Logger LOG = LoggerFactory.getLogger(GraphReportImpl.class);

    private JsonArray report;

    public GraphReportImpl() {
        this.report = new JsonArray();
    }

    @Override
    public void addRecord(JsonObject record) {
        synchronized (report) {
            report.add(record);
        }
    }

    @Override
    public JsonObject dumpReportData(int modelVersion, boolean deepCopy) {
        // Currently, modelVersion is not accounted
        return new JsonObject()
                .put("modelVersion", CURRENT_REPORT_MODEL_VERSION)
                .put("records", dumpRecords(deepCopy));
    }

    @SuppressWarnings("unchecked")
    @Override
    public JsonArray dumpRecords(boolean deepCopy) {
        synchronized (report) {
            if (deepCopy) {
                return report.copy();
            } else {
                return new JsonArray(new ArrayList<>(report.getList()));
            }
        }
    }

    /** Get direct link to mutable underlying report data. Accesses shall be {@code synchronized}
     *
     * @return Underlying report data
     */
    public JsonArray getRecords() {
        return report;
    }

    @Override
    public void clear() {
        synchronized (report) {
            report.clear();
        }
    }

    @Override
    public void restoreReportData(JsonObject dump) {
        int modelVersion = dump.getInteger("modelVersion", CURRENT_REPORT_MODEL_VERSION);
        if (modelVersion != CURRENT_REPORT_MODEL_VERSION) {
            throw new IllegalArgumentException("Report model version " + modelVersion + " not supported");
        }
        restoreRecords(dump.getJsonArray("records"));
    }

    @Override
    public void restoreRecords(JsonArray reportRecords) {
        synchronized (report) {
            report.clear();
            for (Object obj : reportRecords) {
                report.add(obj);
            }
        }
    }


    @Override
    public void addMessage(String type, JsonObject sourceComponent, String message) {
        addMessage(type, sourceComponent, message, null);
    }

    public void addMessage(String type, JsonObject sourceComponent, String message, Map<String, Object> moreElements) {
        JsonObject record = create(sourceComponent, type, message);
        if (moreElements != null) {
            record.getMap().putAll(moreElements);
        }
        String logName = record.getString(COMPONENT);
        if (GraphDataConstants.UNKNOWN.equals(logName) && record.containsKey(GRAPH_ID)) {
            logName = record.getString(GRAPH_ID);
        }
        LOG.error("{} in {}: {}", type, logName, message);
        addRecord(record);
    }

    @Override
    public void mandatoryValueMissed(JsonObject sourceComponent, String property) {
        addMessage(CONF_ERROR, sourceComponent,
            "Mandatory property missed. Property: " + property
        );
    }

    @Override
    public void referenceNotFound(JsonObject sourceComponent, String reference) {
        addMessage(CONF_ERROR, sourceComponent,
            "Reference was not found. Reference: " + reference
            );
    }

    @Override
    public void configurationFileNotFound(JsonObject sourceComponent, String configurationFile) {
        addMessage(CONF_ERROR, sourceComponent,
            "Configuration file was not found. File: " + configurationFile
        );
    }

    @Override
    public void componentDuplicated(JsonObject sourceComponent, JsonObject duplicatedComponent) {
        addMessage(CONF_ERROR, duplicatedComponent,
            "Component duplicated. Id: " + duplicatedComponent.getString(F_ID)
        );
    }

    @Override
    public void conventionNotMatched(JsonObject sourceComponent, String pattern, String value) {
        addMessage(CONF_ERROR, sourceComponent,
            "Value does not match the convention. Value: '" + value + "'. Pattern: " + pattern
            );
    }

    @Override
    public void exceptionThrown(JsonObject sourceComponent, Exception exception) {
        addMessage(EXCEPTION, sourceComponent,
                "Exception was thrown while handling '" + sourceComponent.getString("id")
                        + "': " + exception.getMessage() + "\nStacktrace:\n" + Arrays.asList(exception.getStackTrace())
        );
        LOG.error("Exception: ", exception);
    }

    @Override
    public void internalError(String message) {
        addMessage(ERROR, null, message);
    }

    @Override
    public void mergingError(JsonObject sourceDesc, String message) {
        String desc = InvalidGraphException.descToName(sourceDesc);
        addMessage(MERGE_ERROR, null, message, Collections.singletonMap(GRAPH_ID, desc));
    }

    @Override
    public void mergingError(JsonObject sourceDesc, Exception exception) {
        String desc = InvalidGraphException.descToName(sourceDesc);
        String message;
        if (exception instanceof InvalidGraphException) {
            message = ((InvalidGraphException)exception).getReason();
        } else {
            message = exception.getMessage();
        }
        addMessage(MERGE_ERROR, null, message, Collections.singletonMap(GRAPH_ID, desc));
    }


    protected JsonObject create(JsonObject component, String type, String message) {
        String componentName;
        if (component != null) {
            componentName = component.getString(F_ID, GraphDataConstants.UNKNOWN);
        } else {
            componentName = GraphDataConstants.UNKNOWN;
        }
        JsonObject result = new JsonObject()
            .put(TYPE, type)
            .put(MESSAGE, component==null ? message : express(component.getMap(), message))
            .put(COMPONENT, componentName)
            ;
        return result;
    }

    protected String express(Map<String, Object> values, String message) {
        if (! message.contains("$")) {
            return message;
        }
        // Substitute variables with data from values
        StringSubstitutor sub = new StringSubstitutor(values);
        return sub.replace(message);
    }

}
