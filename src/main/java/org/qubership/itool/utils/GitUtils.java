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

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

public class GitUtils {

    public static String buildRepositoryLink(JsonObject component, String fileName, JsonObject config) {
        String relativePath = FSUtils.relativePath(component, fileName);
        return buildRepositoryLinkRelative(component, relativePath, config);
    }

    public static String buildRepositoryLinkRelative(JsonObject component, String relativePath, JsonObject config) {
        String repositoryLink = component.getString("repository").replaceAll("^(\\S+).git$", "$1");
        String branchFromComponent = (String) JsonPointer.from("/details/releaseBranch").queryJson(component);
        if (branchFromComponent == null) {
            branchFromComponent = ConfigUtils.getConfigValue(ConfigProperties.RELEASE_BRANCH_POINTER, config);
        }
        String branch = "/-/blob/" + branchFromComponent + "/";
        String subDir = component.getString("repositorySubDir", "");
        if (! subDir.isEmpty()) {
            subDir = subDir + "/";
        }
        return repositoryLink + branch + subDir + relativePath;
    }
}
