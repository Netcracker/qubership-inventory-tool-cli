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
import io.vertx.core.Promise;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonObject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public interface GitAdapter {

    WorkerExecutor getWorkerExecutor();

    void setWorkerExecutor(WorkerExecutor executor);

    void gitAddHandler(Git repo, String dir, Promise promise);

    Future<Void> gitAdd(Git repo, String dir);

    void gitCommitHandler(Git repo, String message, Promise promise);

    Future<Object> gitStatusCheck(Git repo, Predicate<Status> statusPredicate);

    Future<Void> gitCommit(Git repo, String message);

    Future<Status> gitStatus(Git repo);

    Future<Void> gitRm(Git repo, Collection<String> files);

    void submoduleUpdateHandler(Git repository, Promise promise);

    Future<Void> submoduleUpdate(Git repo);

    List<Future> bulkSubmoduleAdd(Git superRepo, List<Map<String, JsonObject>> components);

    void submoduleAddHandler(Git superRepo, JsonObject component, JsonObject domain, Promise promise);

    Future<Void> submoduleAdd(Git superRepo, JsonObject component, JsonObject domain);

    void initSuperRepoHandler(String directoryPath, Promise promise);

    void openRepositoryHandler(Promise p);

    Future<Git> openSuperrepository();

    void submodulesCheckoutHandler(Git repository, String branch, List<JsonObject> components, Promise promise);

    Future<Void> submodulesCheckout(Git superrepo, String branch, List<JsonObject> components);

    Future<Void> branchCheckout(Git repository, String branch);

    void prepareSuperRepoHandler(Promise<Git> promise);

    Future switchSuperRepoBranch(Git superRepository, String superRepositoryBranch);

    Future<Git> prepareSuperRepository();

}
