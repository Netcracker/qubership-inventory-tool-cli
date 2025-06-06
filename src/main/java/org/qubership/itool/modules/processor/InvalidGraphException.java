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

import io.vertx.core.json.JsonObject;

import org.apache.commons.lang3.StringUtils;

import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graph.GraphDataConstants;

@SuppressWarnings("serial")
public class InvalidGraphException extends IllegalArgumentException {

    private final String reason;

    public InvalidGraphException(JsonObject graphDesc, String reason) {
        super("Graph " + descToName(graphDesc) + " is invalid: " + reason);
        this.reason = reason;
    }

    public InvalidGraphException(Graph graph, String reason) {
        super("Graph " + graphToName(graph) + " is invalid: " + reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }


    public static String descToName(JsonObject metaInfo) {
        if (metaInfo.getBoolean(MergerApi.P_IS_APPLICATION, false)) {
            String appVersion = metaInfo.getString(MergerApi.P_APP_VERSION);
            if (StringUtils.isEmpty(appVersion)) {
                appVersion = GraphDataConstants.UNKNOWN;
            }
            return "APPLICATION:" + metaInfo.getString(MergerApi.P_APP_NAME) + ":" + appVersion;
        }
        String fileName = metaInfo.getString(GraphMerger.P_FILE_NAME);
        if (fileName != null) {
            return fileName;
        }
        return GraphDataConstants.UNKNOWN;
    }

    public static String graphToName(Graph graph) {
        JsonObject root = graph.getVertex(Graph.V_ROOT);
        if (root != null) {
            JsonObject meta = root.getJsonObject("meta");
            if (meta != null) {
                return meta.toString();
            }
        }
        return "<unidentified graph>";
    }

}
