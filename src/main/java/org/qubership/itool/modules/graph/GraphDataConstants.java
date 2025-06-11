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

import java.util.Map;
import java.util.Set;

/**
 * Well-known constants used when filling graph data.
 *
 * See also: common attributes and Common vertex types in {@link Graph}
 */
public interface GraphDataConstants {

    String UNKNOWN = "unknown";

    String MISSED_IN_INVENTORY_FILE = "Missed in inventory file";
    String NOT_REQUIRED = "not required";

    /** How developers (or our processing tasks) may write "no" in inventory files. */
    Set<String> NOS_TO_RECOGNIZE = Set.of(
        MISSED_IN_INVENTORY_FILE, MISSED_IN_INVENTORY_FILE.toLowerCase(), NOT_REQUIRED, "no", "n/a", "-"
    );

    /** Names and JSON pathes of recognized dependency types of components */
    Map<String, String> COMP_DEPENDENCY_TYPES = Map.of(
            "startup", "/details/dependencies/startup",
            "mandatory", "/details/dependencies/mandatory",
            "optional", "/details/dependencies/optional");


    String DOMAIN_ID_PREFIX = "D_";

    String UNKNOWN_DOMAIN_NAME = "orphans"; // Does not start with DOMAIN_ID_PREFIX

}
