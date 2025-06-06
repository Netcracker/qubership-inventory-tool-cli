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

import org.apache.commons.lang3.StringUtils;

/** A filter for artifact snapshots. Snapshots must exactly fit both {@code classifier} AND {@code extension} */
public class SnapshotFilter {
    private final String classifier;
    private final String extension;

    public SnapshotFilter(String classifier, String extension) {
        this.classifier = classifier;
        this.extension = extension;
    }

    @Override
    public String toString() {
        return "SnapshotFilter [classifier=" + classifier + ", extension=" + extension + "]";
    }

    public boolean matches(Snapshot snapshot) {
        return matches(classifier, snapshot.getClassifier())
            && matches(extension, snapshot.getExtension());
    }

    public static boolean matches(String s1, String s2) {
        return StringUtils.defaultString(s1).equals(StringUtils.defaultString(s2));
    }

    public String getClassifier() {
        return classifier;
    }

    public String getExtension() {
        return extension;
    }

}
