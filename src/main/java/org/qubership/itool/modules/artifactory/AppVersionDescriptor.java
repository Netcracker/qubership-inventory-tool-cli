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

package org.qubership.itool.modules.artifactory;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class AppVersionDescriptor {

    private String id;
    private String appName;
    private String appVersion;

    public AppVersionDescriptor() {
    }

    public AppVersionDescriptor(String id, String appName, String appVersion) {
        this.id = id;
        this.appName = appName;
        this.appVersion = appVersion;
    }

    public String asArtifactId() {
        return getAppName() + ":" + getAppVersion();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }


    @Override
    public int hashCode() {
        return Objects.hash(appName, appVersion /*, id */);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AppVersionDescriptor other = (AppVersionDescriptor) obj;
        return Objects.equals(appName, other.appName) && Objects.equals(appVersion, other.appVersion)
                /* && Objects.equals(id, other.id) */;
    }

    @Override
    public String toString() {
//        return ToStringBuilder.reflectionToString(this);
        return "AppVersionDescriptor" + ToStringBuilder.reflectionToString(this, ToStringStyle.NO_CLASS_NAME_STYLE, false);
    }

}
