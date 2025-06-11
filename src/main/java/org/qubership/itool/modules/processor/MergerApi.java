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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import io.vertx.core.json.JsonObject;

public interface MergerApi {

    //------------------------------------------------------
    // Meta-info parameter keys

    // Shall be provided explicitly

    String P_IS_APPLICATION = "isApplication";
    String P_APP_NAME = "appName";
    String P_APP_VERSION = "appVersion";
    String P_IS_NAMESPACE = "isNameSpace";
    String P_NAMESPACE_NAME = "nameSpaceName";

    // Filled automatically when walking directories
    String P_FILE_NAME = "fileName";

    //------------------------------------------------------
    // Public merger methods

    /**
     * Merge dumps of component graphs stored in given directory (including its sub-directories)
     * into a new graph dump. Designed to process set if components included into a single application.
     * <b>Source directory shall not contain application dumps!</b>
     *
     * @param sourceDirectory Storage of source dumps
     * @param targetDesc Description of merging target and merging features. Supported keys:
     * <ul><li>P_IS_APPLICATION: Boolean
     * <li>P_APP_NAME: String
     * <li>P_APP_VERSION: String
     * <li>P_IS_NAMESPACE: Boolean
     * <li>P_NAMESPACE_NAME: String
     * </ul>
     *
     * @return Dump of merged graph
     * @throws IOException IO or parsing exception
     * @throws InvalidGraphException Logical error: invalid graph or incompatible combination
     * of source/target data/meta-info.
     */
    JsonObject mergeComponentDumps(Path sourceDirectory, JsonObject targetDesc)
            throws IOException, InvalidGraphException;

    /**
     * Merge graph dumps (either components or applications) into a new graph dump.
     * Can merge components into an application or merge arbitrary set of components/applications
     * into overview graph.
     *
     * @param sourceDumps Source dumps with meta-info.
     * Supported keys of {@link DumpAndMetainfo#getDump()} :
     * <ul><li>P_IS_APPLICATION: Boolean
     * <li>P_APP_NAME: String
     * <li>P_APP_VERSION: String
     * <li>P_IS_NAMESPACE: Boolean
     * <li>P_NAMESPACE_NAME: String
     * </ul>
     *
     * <b>Dumps are copied shallowly and may be altered later by manipulations with resulting graph,
     * so they should not be reused after merging!</b>
     *
     * @param targetDesc Same as {@code targetDesc} in {@link #mergeComponentDumps(Path, JsonObject)}
     * @return Dump of merged graph
     * @throws InvalidGraphException Logical error: incompatible combination of source/target data/meta-info
     */
    JsonObject mergeDumps(List<DumpAndMetainfo> sourceDumps, JsonObject targetDesc)
            throws InvalidGraphException;

}
