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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

public class GraphClassifierImpl implements GraphClassifier {

    private String id;
    private transient boolean withReport;
    private List<String> departmentIds;
    private List<String> domainIds;
    private List<String> releaseVersionIds;
    private List<String> applicationVersionIds;

    public GraphClassifierImpl(String id, boolean withReport, List<String> departmentIds,
                               List<String> domainIds, List<String> releaseVersionIds,
                               List<String> applicationVersionIds) {
        this.withReport = withReport;
        this.id = id;
        this.departmentIds = departmentIds;
        this.domainIds = domainIds;
        this.releaseVersionIds = releaseVersionIds;
        this.applicationVersionIds = applicationVersionIds;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public boolean isWithReport() {
        return withReport;
    }

    @Override
    public List<String> getDepartmentIds() {
        return departmentIds;
    }

    @Override
    public List<String> getDomainIds() {
        return domainIds;
    }

    @Override
    public List<String> getReleaseVersionIds() {
        return releaseVersionIds;
    }

    @Override
    public List<String> getApplicationVersionIds() {
        return applicationVersionIds;
    }

    @Override
    public List<String> getUnprocessedAppVerIds() {
        // TODO: implement
        throw new AbstractMethodError();
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GraphClassifier) {
            return this.id.equals(((GraphClassifier)obj).getId());
        }
        return false;
    }

    @Override
    public String toString() {
        return "GraphClassifierImpl" + ToStringBuilder.reflectionToString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
    }

}
