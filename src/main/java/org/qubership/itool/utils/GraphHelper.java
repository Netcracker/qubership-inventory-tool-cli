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

package org.qubership.itool.utils;

import org.qubership.itool.modules.graph.Graph;
import io.vertx.core.json.JsonObject;

import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.gremlin2.P.within;
import static org.qubership.itool.modules.gremlin2.graph.__.out;

public class GraphHelper {

    public static Boolean isComponentAMicroservice(Graph graph, JsonObject component) {
        return null != graph.traversal().V(component.getString(F_ID))
                .where(out("directory").glob("/helm-templates/**/resource-profiles/*")
                                .has("name", within("dev.yaml", "prod.yaml", "dev-ha.yaml", "prod-nonha.yaml")))
                .next();
    }

}
