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

import java.util.Objects;

// This class is a part of public API! Do not move or rename it!
public class FalloutDto {

    String appName;
    String appVersion;
    String mSName;

    public FalloutDto() {
    }

    public FalloutDto(String appName, String appVersion, String mSName) {
        this.appName = appName;
        this.appVersion = appVersion;
        this.mSName = mSName;
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

    public String getMSName() {
        return mSName;
    }

    public void setMSName(String mSName) {
        this.mSName = mSName;
    }

    @Override
    public String toString() {
        return "FalloutDto [appName=" + appName + ", appVersion=" + appVersion + ", mSName=" + mSName + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(appName, appVersion, mSName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FalloutDto other = (FalloutDto) obj;
        return Objects.equals(appName, other.appName) && Objects.equals(appVersion, other.appVersion)
                && Objects.equals(mSName, other.mSName);
    }

}
