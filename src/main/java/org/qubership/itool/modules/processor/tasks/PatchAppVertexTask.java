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

package org.qubership.itool.modules.processor.tasks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graph.GraphDataConstants;
import org.qubership.itool.modules.processor.InvalidGraphException;
import org.qubership.itool.modules.report.GraphReport;

import io.vertx.core.json.JsonObject;

import static org.qubership.itool.modules.processor.MergerApi.*;
import static org.qubership.itool.modules.graph.Graph.*;

/**
 * <p>Patch Application vertex if its version has been changed (usually from unknown to some known).
 * Intended to be applied to source graphs before merging.
 *
 * <p>Pre-condition: This task is applied to Application graph (otherwise, it does nothing),
 * and that graph has some application vertices.
 *
 * <p>Actions:
 * <ul><li>If application graph contains no application vertices, one is created
 * (already implemented in {@link CreateAppVertexTask}).
 * <li>If application graph contains some application vertex with given name but different
 * (not only unknown) version, that version is patched.
 * <li>If application graph contains some application vertices but neither has given name,
 * NO new application vertices is created, and old ones are not patched.
 * </ul>
 */
public class PatchAppVertexTask implements GraphProcessorTask {

    private static final Logger LOG = LoggerFactory.getLogger(PatchAppVertexTask.class);

    protected final JsonObject desc;
    protected final String appName;
    protected final String appVersion;
    protected boolean disabled;

    public PatchAppVertexTask(JsonObject desc) {
        this.desc = desc;
        this.appName = desc.getString(P_APP_NAME);
        this.appVersion = desc.getString(P_APP_VERSION);

        if (   ! desc.getBoolean(P_IS_APPLICATION, false)
            || StringUtils.isEmpty(appVersion)
            || GraphDataConstants.UNKNOWN.equals(appVersion)
            || StringUtils.isEmpty(appName))
        {
            disabled = true;
        }
    }

    @Override
    public void process(Graph graph) throws InvalidGraphException {
        if (disabled) {
            LOG.info("Skipping task");
            return;
        }

        long startTime = System.nanoTime();
        LOG.info("Starting task {}", getClass().getSimpleName());

        List<JsonObject> existingApps = graph.traversal().V().hasType(Graph.V_APPLICATION).toList();
        boolean requiredAppFound = false;
        List<String> otherAppNames = new ArrayList<>();
        String appVertexId = String.join(":", "application", appName, appVersion);

        for (JsonObject existingApp: existingApps) {
            String existingAppId = existingApp.getString(F_ID);
            String existingAppName = existingApp.getString(Graph.F_NAME);
            if (appName.equals(existingAppName)) {
                requiredAppFound = true;
                String version = existingApp.getString(Graph.F_VERSION);
                if (appVersion.equals(version)) {
                    LOG.info("Source graph contains app '{}'", existingAppId);
                } else {
                    LOG.warn("Source graph contains app '{}', needed version is '{}'. Patching it.",
                            existingAppId, appVersion);
                    JsonObject originalApp = existingApp.copy();
                    originalApp.put(Graph.F_VERSION, appVersion);
                    graph.relocateVertex(originalApp, appVertexId);
                }
            } else {
                LOG.warn("Source graph contains app with name other than '{}': '{}'",
                        appName, existingAppId);
                otherAppNames.add(existingAppName);
            }
        }
        if (!requiredAppFound) {
            GraphReport report = graph.getReport();
            if (report != null) {
                JsonObject info = new JsonObject();
                info.put("id", appVertexId);
                report.mergingError(desc, "Application name '" + appName + "' expected, found: " + otherAppNames.toString());
            }
        }

        long endTime = System.nanoTime();
        LOG.info("Task completed {} in {}", getClass().getSimpleName(), Duration.ofNanos(endTime - startTime));
    }

}
