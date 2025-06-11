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

package org.qubership.itool.modules.git;

import io.vertx.core.Future;

import org.apache.commons.lang3.tuple.Triple;
import org.eclipse.jgit.api.Git;

import java.nio.file.Path;
import java.util.List;

public interface GitFileRetriever {

    Future<Void> copyFilesFromReleases(String release1, String release2, List<Path> files);

    Future<Triple<Path, String, String>> getFileInfo(String sourceRelease, String targetRelease, Path path);

    List<Future> copyFilesFromRepo(Git repo, String release, List<Path> files);

    Path buildRepoPath(Git repo, Path basePath);

    Path buildDiffOutputPath(String release, Path basePath);
}
