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

public interface ConfigProperties {

    String SUPER_REPOSITORY_DIR_POINTER = "/git/superRepositoryDir";
    String SUPER_REPOSITORY_MODULES_DIR_POINTER = "/git/superRepositoryModulesDir";
    String SUPER_REPOSITORY_URL_POINTER = "/git/superRepositoryUrl";
    String RELEASE_POINTER = "/release";
    String PRIOR_RELEASE_POINTER = "/priorRelease";
    String RELEASE_BRANCH_POINTER = "/git/releaseBranch";
    String DEFAULT_MAIN_BRANCH_POINTER = "/git/defaultMainBranch";
    String UPLOAD_CONFLUENCE_PAGES_POINTER = "/confluence/uploadConfluencePages";
    String CONFLUENCE_SPACE_POINTER = "/confluence/space";
    String CONFIG_PATH_POINTER = "/configPath";
    String PROFILE_POINTER = "/profile";
    String DISABLED_FEATURES_POINTER = "/disabledFeatures";
    String SAVE_PROGRESS = "saveProgress";

    String QUERY_PROGRESS_PATH_POINTER = "/query/progressPath";
    String QUERY_STEP_POINTER = "/query/step";
    String QUERY_FILE_POINTER = "/query/file";
    String QUERY_APP_NAME_POINTER = "/query/appName";
    String QUERY_APP_VERSION_POINTER = "/query/appVersion";

    String PASSWORD_PROPERTY = "password";
    String PASSWORD_SOURCE_PROPERTY = "passwordSource";
    String OFFLINE_MODE = "offlineMode";
    String DOCKER_MODE = "dockerMode";

    String DEFAULT_RELEASE = "default";
    String RELEASES_DIR = "releases";
    String UPLOAD_KEY_ALL = "all";
    String UPLOAD_KEY_NONE = "none";

    String ARTIFACTORY_SERVER_POINTER = "/artifactory/server";
    String ARTIFACTORY_STORAGE_POINTER = "/artifactory/storage";

    String PLANTUML_URL_POINTER = "/plantUMLUrl";

    String APPDB_URL_POINTER = "/appDB/url";
    String APPDB_AUTH_METHOD_POINTER = "/appDB/auth/method";
    String APPDB_MAVEN_ENTRY_POINTER = "/appDB/maven/entry";

}
