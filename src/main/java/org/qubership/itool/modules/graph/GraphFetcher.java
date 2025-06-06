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

import org.qubership.itool.modules.artifactory.AppVersionDescriptor;
import org.qubership.itool.modules.artifactory.GraphSnapshot;
import java.util.*;

public interface GraphFetcher {

    default AppVersionDescriptor resolveAppVersion(String appVersionId) {
        return null;
    }

    /**
     * @param appVersionIds Artifact ids to resolve
     *
     * @return A map with some of the provided keys and possibly {@code null} values
     */
    default Map<String, AppVersionDescriptor> resolveAppVersions(Collection<String> appVersionIds) {
        Map<String, AppVersionDescriptor> result = new LinkedHashMap<>();
        for (String appVersionId: appVersionIds) {
            AppVersionDescriptor desc = resolveAppVersion(appVersionId);
            result.put(appVersionId, desc);
        }
        return result;
    }

    default GraphSnapshot fetchGraphDumpByAppVersion(AppVersionDescriptor appVersionDesc) {
        return null;
    }

    /**
     * @param appVersions Artifact descriptors to fetch
     *
     * @return A map with some of the provided keys, possible {@code null} values or
     * values without graphDump (they may still carry failure details).
     */
    default Map<AppVersionDescriptor, GraphSnapshot> fetchGraphDumpsByAppVersions(Collection<AppVersionDescriptor> appVersions) {
        Map<AppVersionDescriptor, GraphSnapshot> result = new LinkedHashMap<>();
        for (AppVersionDescriptor appVersion: appVersions) {
            GraphSnapshot graphSnapshot = fetchGraphDumpByAppVersion(appVersion);
            result.put(appVersion, graphSnapshot);
        }
        return result;
    }

    default GraphClassifier resolveGraphClassifier(String graphClassifierId) {
        return null;
    }

    default GraphSnapshot fetchGraphDumpByClassifier(GraphClassifier classifier) {
        return null;
    }

    default boolean persistGraphByClassifier(GraphClassifier graphClassifier, GraphSnapshot graphSnapshot,
            Map<AppVersionDescriptor, GraphSnapshot> sources, Map<AppVersionDescriptor, GraphSnapshot> unprocessedAppIds)
    {
        return false;
    }

    /**
     * Get all the application version ids: not only directly added, but also those included into
     * departments, domains, etc...
     *
     * @param graphClassifier Source classifier
     * @return All the application version ids
     */
    default List<AppVersionDescriptor> fetchAllApplicationVersionIds(GraphClassifier graphClassifier) {
        return null;
    }

}
