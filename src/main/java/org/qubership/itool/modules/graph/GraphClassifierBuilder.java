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

public interface GraphClassifierBuilder {

    GraphClassifierBuilder setId(String id);

    /* @see GraphClassifier#isWithReport() */
    GraphClassifierBuilder setWithReport(boolean withReport);

    GraphClassifier build();

    GraphClassifierBuilder setDepartmentIds(List<String> departmentIds);

    GraphClassifierBuilder setDomainIds(List<String> domainIds);

    GraphClassifierBuilder setReleaseVersionIds(List<String> releaseVersionIds);

    GraphClassifierBuilder setApplicationVersionIds(List<String> applicationVersionIds);

    GraphClassifierBuilder addDepartment(String departmentId);

    GraphClassifierBuilder addDomain(String domainId);

    GraphClassifierBuilder addReleaseVersionId(String releaseVersionId);

    GraphClassifierBuilder addApplicationVersionId(String applicationVersionId);

}
