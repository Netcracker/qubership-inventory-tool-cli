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

/**
 * Locator for resources related to given application or component on given {@link ArtifactoryConfig server}
 */
public class ResourceLocator {

    /** If not "latest", try this! */
    public static final String DEFAULT_APP_VERSION = "main-SNAPSHOT";
    public static final String SNAPSHOT_VERSION_EXTRACTION_PATTERN = "^([-\\S]+)\\-(?:\\d{8}\\.\\d{6}-\\d+)$";

    private final ArtifactoryConfig artifactory;
    private final ArtifactKind kind;
    private final String groupId;
    private final String groupIdNormalized;
    private final String artifactId;

    public ResourceLocator(ArtifactoryConfig artifactory, ArtifactKind kind, String groupId, String artifactId) {
        this.artifactory = artifactory;
        this.kind = kind;
        this.groupId = groupId;
        this.groupIdNormalized = groupId != null ? groupId.replace('.', '/') + '/' : "";
        this.artifactId = artifactId;
    }

    public static ResourceLocator forApplication(ArtifactoryConfig artifactory, String groupId, String applicationId) {
        return new ResourceLocator(artifactory, ArtifactKind.APPLICATION, groupId, applicationId);
    }

    public static ResourceLocator forComponent(ArtifactoryConfig artifactory, String groupId, String componentId) {
        return new ResourceLocator(artifactory, ArtifactKind.COMPONENT, groupId, componentId);
    }


    public ArtifactKind getKind() {
        return kind;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getBaseUrl() {
        return artifactory.server + '/' + artifactory.storage + '/'
                + groupIdNormalized
                + artifactId + '/';
    }

    public String getBaseUrl(String versionId) {
        return artifactory.server + '/' + artifactory.storage + '/'
                + groupIdNormalized
                + artifactId + '/'
                + versionId + '/';
    }

    /**
     * Construct URL to the artifact in artifactory
     * @param versionId version ID (folder of the artifact), could be snapshot version, e.g. "main-SNAPSHOT",
     *                  or release version ("release-2024-2-1.011.017-RELEASE")
     * @param snapshotVersionId specific version of the artifact, e.g. "main-20240611.150219-1211" or "release-2024-2-1.011.017-RELEASE"
     * @param artifactClassifier Classifier of the artifact, e.g. "-graph"
     * @param extension File name extension, e.g. ".json"
     * @return URL of the artifact
     */
    public String getArtifactUrl(String versionId, String snapshotVersionId, String artifactClassifier, String extension) {
        if (StringUtils.isNotEmpty(artifactClassifier)) {
            return getBaseUrl(versionId)
                    + artifactId + '-' + snapshotVersionId + '-' + artifactClassifier + '.' + extension;
        } else {
            return getBaseUrl(versionId)
                    + artifactId + '-' + snapshotVersionId + '.' + extension;
        }
    }

    /**
     * Construct URL to the artifact in artifactory based on version and snapshot descriptor
     * @param versionId Artifact version
     * @param snapshot artifact snapshot descriptor
     * @return URL of the artifact
     */
    public String getArtifactUrl(String versionId, Snapshot snapshot) {
        return getArtifactUrl(versionId, snapshot.getSnapshotId(), snapshot.getClassifier(), snapshot.getExtension());
    }

    /**
     * Construct URL to the release artifact in artifactory by version
     * Similar to getArtifactUrl() where versionId equals snapshotVersionId
     * @param versionId Release version
     * @param descriptor SnapshotFilter with classifier and file extension
     * @return URL of the release artifact
     */
    public String getReleaseArtifactUrl(String versionId, SnapshotFilter descriptor) {
        // Append something like "-graph.json.gz" :
        return getArtifactUrl(versionId, versionId, descriptor.getClassifier(), descriptor.getExtension());
    }

    /**
     * Construct URL of the snapshot artifact in artifactory by version and snapshot filter descriptor
     * Similar to getArtifactUrl(), but snapshotVersionId is extracted from versionId to provide proper folder
     * @param versionId Release version
     * @param descriptor SnapshotFilter with classifier and file extension
     * @return URL of the snapshot artifact
     */
    public String getSnapshotArtifactUrl(String versionId, SnapshotFilter descriptor) {
        String snapshotVersion = getSnapshotFromVersion(versionId);
        return getArtifactUrl(snapshotVersion, versionId, descriptor.getClassifier(), descriptor.getExtension());
    }

    public static String getSnapshotFromVersion(String versionId) {
        return versionId.replaceAll(SNAPSHOT_VERSION_EXTRACTION_PATTERN, "$1-SNAPSHOT");
    }

    @Override
    public String toString() {
        return "ResourceLocator [artifactory=" + artifactory
                + ", kind=" + kind + ", groupId=" + groupId
                + ", artifactId=" + artifactId + "]";
    }

}
