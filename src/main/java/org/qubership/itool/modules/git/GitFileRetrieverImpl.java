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

import org.qubership.itool.modules.report.GraphReport;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GitFileRetrieverImpl implements GitFileRetriever {
    protected static final Logger LOG = LoggerFactory.getLogger(GitFileRetrieverImpl.class);

    private GitAdapter gitAdapter;
    private JsonObject config;
    private Vertx vertx;
    private GraphReport report;

    public GitFileRetrieverImpl(GitAdapter gitAdapter, JsonObject config, Vertx vertx, GraphReport report) {
        this.gitAdapter = gitAdapter;
        this.config = config;
        this.vertx = vertx;
        this.report = report;
    }

    @Override
    public Future copyFilesFromReleases(String sourceRelease, String targetRelease, List<Path> files) {
        return gitAdapter.openSuperrepository()
                .compose(repo -> gitAdapter.branchCheckout(repo, sourceRelease)
                        .compose(r -> {
                            List<Future> copyFutures = copyFilesFromRepo(repo, sourceRelease, files);
                            return CompositeFuture.join(copyFutures);
                        })
                        .compose(r -> gitAdapter.branchCheckout(repo, targetRelease)
                                .onFailure(e -> report.internalError("Failed to checkout branch "
                                        + targetRelease + ": " + ExceptionUtils.getStackTrace(e)))
                                .compose(res -> {
                                    List<Future> copyFutures = copyFilesFromRepo(repo, targetRelease, files);
                                    return CompositeFuture.join(copyFutures)
                                            .onFailure(e -> report.internalError("Some files were not copied: "
                                                    + ExceptionUtils.getStackTrace(e)));
                                })
                        )
                        .onComplete(r -> repo.close())
                );
    }

    @Override
    public Future<Triple<Path, String, String>> getFileInfo(String sourceRelease, String targetRelease, Path path) {
        String sourcePath = buildDiffOutputPath(sourceRelease, path).toString();
        String targetPath = buildDiffOutputPath(targetRelease, path).toString();

        Future<String> sourceFuture = vertx.fileSystem().readFile(sourcePath)
                .map(v -> v.toString())
                .recover(r -> Future.succeededFuture(
                        new JsonObject().put("warning", sourcePath + " file was not found").encodePrettily()));
        Future<String> targetFuture = vertx.fileSystem().readFile(targetPath)
                .map(v -> v.toString())
                .recover(r -> Future.succeededFuture(
                        new JsonObject().put("warning", targetPath + " file was not found").encodePrettily()));
        Future<Triple<Path, String, String>> result = CompositeFuture.join(sourceFuture, targetFuture)
                .map(r -> Triple.of(path, sourceFuture.result(), targetFuture.result()));
        return result;
    }

    @Override
    public List<Future> copyFilesFromRepo(Git repo, String release, List<Path> files) {
        List<Future> copyFutures = new ArrayList<>();
        for (Path path : files) {
            Future<Void> futureFile = copyFromRepo(repo, release, path)
                    .recover(e -> {
                        report.internalError("Couldn't copy file " + path.toString()
                                + " from repository " + repo.getRepository().getDirectory() + ": "
                                + ExceptionUtils.getStackTrace(e));
                        return Future.succeededFuture();
                    });
            copyFutures.add(futureFile);
        }
        return copyFutures;
    }

    private Future<Void> copyFromRepo(Git repo, String release, Path basePath) {
        Path repoPath = buildRepoPath(repo, basePath);
        Path newPath = buildDiffOutputPath(release, basePath);
        return copyWithParents(repoPath, newPath);
    }

    private Future<Void> copyWithParents(Path from, Path to) {
        return vertx.fileSystem().exists(to.getParent().toString())
                .compose(r -> {
                    if (!r.booleanValue()) {
                        return vertx.fileSystem().mkdirs(to.getParent().toString());
                    }
                    return Future.succeededFuture();
                })
                .compose(r -> vertx.fileSystem().copy(from.toString(), to.toString()));
    }

    @Override
    public Path buildRepoPath(Git repo, Path basePath) {
        return repo.getRepository().getDirectory().toPath().getParent().resolve(basePath);
    }

    @Override
    public Path buildDiffOutputPath(String release, Path basePath) {
        return Path.of("output",  "diff", release, basePath.getFileName().toString());
    }

    private Future<Map<Path,String>> getFilesFromRepository(List<Path> files, Git repo) {

        Future<Map<Path,String>> result;
        result = Future.future(p -> {
            Map<Path, Future<String>> filesContents = new HashMap<>();
            Map<Path, String> endResult = new HashMap<>();
            Path repoPath = repo.getRepository().getDirectory().toPath().getParent();
            for (Path path : files) {
                Future<String> futureFile = vertx.fileSystem().readFile(repoPath.resolve(path).toString())
                        .map(v -> v.toString());
                filesContents.put(path, futureFile);
            };
            CompositeFuture.join(filesContents.values().stream().collect(Collectors.toList()))
                    .onComplete(r -> {
                        for (Path path: filesContents.keySet()) {
                            if (filesContents.get(path).succeeded()) {
                                endResult.put(path, filesContents.get(path).result());
                            } else {
                                endResult.put(path, null);
                            }
                        }
                        p.complete(endResult);
                    });
        });

        return result;
    }

}
