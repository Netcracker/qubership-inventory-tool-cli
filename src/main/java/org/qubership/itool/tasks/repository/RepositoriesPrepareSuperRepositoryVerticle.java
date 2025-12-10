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

package org.qubership.itool.tasks.repository;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.impl.future.SucceededFuture;
import io.vertx.core.json.JsonObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.qubership.itool.modules.git.GitAdapter;
import org.qubership.itool.modules.git.GitFileRetriever;
import org.qubership.itool.tasks.AbstractAggregationTaskVerticle;
import org.qubership.itool.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.Resource;

import static org.qubership.itool.utils.ConfigProperties.PRIOR_RELEASE_POINTER;
import static org.qubership.itool.utils.ConfigProperties.RELEASE_POINTER;

public class RepositoriesPrepareSuperRepositoryVerticle extends AbstractAggregationTaskVerticle {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RepositoriesPrepareSuperRepositoryVerticle.class);

    @Resource
    @Nullable
    private GitFileRetriever gitFileRetriever;
    @Resource
    @Nullable
    private GitAdapter gitAdapter;

    @Override
    protected String[] features() {
        return new String[]{"repositoryUpdate"};
    }

    @Override
    protected void taskStart(Promise<?> taskPromise) {
        if (gitAdapter == null || gitFileRetriever == null) {
            getLogger().warn("Offline mode. Exitting.");
            taskCompleted(taskPromise);
            return;
        }

        updateRepositories()
        .onFailure(f -> report.internalError("Failed to update repository: " + ExceptionUtils.getStackTrace(f)))
        .onComplete(r -> taskCompleted(taskPromise));
    }

    private Future<Void> updateRepositories() {

        List<Map<String, JsonObject>> jsonObjectList =
                V().hasType("domain").as("domain")
                        .out().hasKeys("directoryPath").as("component")
                        .<JsonObject>select("domain", "component").toList();

        String targetRelease = ConfigUtils.getConfigValue(RELEASE_POINTER, config());
        String sourceRelease = ConfigUtils.getConfigValue(PRIOR_RELEASE_POINTER, config());
        boolean compareRequired = ConfigUtils.isFeatureEnabled("compareReleases", config());

        Future<Void> updateRepoFuture = gitAdapter.prepareSuperRepository()
                .compose(superRepository -> {
                    Future<?> previousReleaseCopyFuture = SucceededFuture.EMPTY;
                    // Copy files from prior release if needed
                    if (compareRequired && sourceRelease != null && !sourceRelease.equals(targetRelease)) {
                        previousReleaseCopyFuture = gitAdapter.switchSuperRepoBranch(superRepository, sourceRelease)
                                .compose(r -> getFilesList())
                                .compose(list -> Future.join(gitFileRetriever.copyFilesFromRepo(
                                        superRepository, sourceRelease, (List<Path>) list)))
                                .recover(throwable -> {
                                    getLogger().warn(
                                            "Comparison with prior release failed, continuing with target release: {}",
                                            throwable instanceof Throwable
                                                    ? ((Throwable) throwable).getMessage()
                                                    : throwable);
                                    return SucceededFuture.EMPTY;
                                });
                    }

                    Future<Void> resultFuture = previousReleaseCopyFuture
                            // Switching to target release
                            .compose(r -> gitAdapter.switchSuperRepoBranch(superRepository, targetRelease))
                            // Adding the missing submodules, if any
                            .compose(r -> {
                                List<Future<?>> futures = gitAdapter.bulkSubmoduleAdd(superRepository, jsonObjectList);
                                return Future.join(futures);
                            })
                            // Performing commit and update of submodules
                            .compose(r -> gitAdapter.gitStatusCheck(superRepository,
                                    s -> !CollectionUtils.isEmpty(s.getAdded())))
                            .compose(r -> {
                                if ((Boolean) r) {
                                    return gitAdapter.gitCommit(superRepository, "New repositories added");
                                } else {
                                    return SucceededFuture.EMPTY;
                                }
                            })
                            .compose(res -> gitAdapter.submoduleUpdate(superRepository)
                                    .onComplete(h -> superRepository.close()));
                    return resultFuture;
                });
        return updateRepoFuture;
    }

    private Future<List<Path>> getFilesList() {
        return vertx
                .fileSystem().readFile(ConfigUtils
                        .getConfigFilePath(config(), "config", "diffConfig.json").toString())
                .map(fileContents -> {
                    JsonObject jsonResult = new JsonObject(fileContents);
                    List<Path> filesArray = jsonResult.getJsonArray("files").stream()
                            .map(str -> Path.of((String) str)).collect(Collectors.toList());
                    return filesArray;
                });
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
