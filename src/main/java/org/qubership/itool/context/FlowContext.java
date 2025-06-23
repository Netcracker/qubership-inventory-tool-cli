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

package org.qubership.itool.context;

import java.io.File;
import java.util.Map;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graph.GraphClassifier;
import org.qubership.itool.modules.report.GraphReport;

public interface FlowContext {

    /* Initialize this */
    void initialize(Vertx vertx, JsonObject config);

    /* Initialize a task, inject resources */
    void initialize(Object task);

    <T> T getResource(Class<T> clazz);

    Map<Class<?>, Object> getResources();

    Graph getGraph();

    GraphReport getReport();

    /* Dump flow data (that is: graph data, report and other associated stuff) into file.
     * Flow control (tasks passed, etc) is not saved. */
    void dumpDataToFile(File folder, String file);

    /* Restore flow data from a dump.
     * Object identities injected into flow tasks are NOT changed, their states may be changed.
     * Flow control is not affected. */
    void restoreData(JsonObject dump);

    Vertx getVertx();

    JsonObject getConfig();

    ClassLoader getTaskClassLoader();

    void setTaskClassLoader(ClassLoader taskClassLoader);

    void setBreakRequested(boolean b);

    boolean isBreakRequested();

    String getFlowInstanceId();

    GraphClassifier getGraphClassifier();

}
