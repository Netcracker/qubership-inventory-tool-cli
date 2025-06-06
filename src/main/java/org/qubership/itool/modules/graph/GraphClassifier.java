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

import java.util.List;

public interface GraphClassifier {

    String getId();

    /*
     * <p>When requesting a graph: do we require its report as well? Obtained graph may
     * contain a report even if it was not requested.
     * <p>When saving a graph: effectively, this flag should be ignored, yet usually
     * no one should save a graph that has lost its report.
     */
    boolean isWithReport();

    List<String> getDepartmentIds();

    List<String> getDomainIds();

    List<String> getReleaseVersionIds();

    List<String> getApplicationVersionIds();

    List<String> getUnprocessedAppVerIds();

}
