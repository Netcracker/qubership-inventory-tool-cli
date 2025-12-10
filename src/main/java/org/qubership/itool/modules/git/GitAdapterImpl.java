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
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.submodule.SubmoduleStatus;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FileUtils;
import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.utils.ConfigProperties;
import org.qubership.itool.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_REPOSITORY;
import static org.qubership.itool.utils.ConfigProperties.LOGIN_PROPERTY;
import static org.qubership.itool.utils.ConfigProperties.SUPER_REPOSITORY_DIR_POINTER;
import static org.qubership.itool.utils.ConfigProperties.SUPER_REPOSITORY_URL_POINTER;

@SuppressWarnings({"rawtypes", "unchecked"})
public class GitAdapterImpl implements GitAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(GitAdapterImpl.class);

    // TODO: @Resource is not working yet, ApplicationContext update required.
    //  Client and Vertx are passed in constructor so far;
    private GraphReport report;
    private Vertx vertx;

    private CredentialsProvider credentialsProvider;
    private JsonObject config;
    private WorkerExecutor executor;

    protected GitAdapterImpl(Vertx vertx, GraphReport report, JsonObject config) {
        this.report = report;
        this.vertx = vertx;

        String password = config.getString("password");
        String login = config.getString(LOGIN_PROPERTY);
        this.credentialsProvider = new UsernamePasswordCredentialsProvider(login, password);

        this.config = config;
    }

    @Override
    public WorkerExecutor getWorkerExecutor() {
        if (executor == null) {
            Integer coresCount = CpuCoreSensor.availableProcessors();
            executor = vertx.createSharedWorkerExecutor("repository-worker-pool",
                    coresCount,
                    15,
                    TimeUnit.MINUTES);
        }
        return executor;
    }

    @Override
    public void setWorkerExecutor(WorkerExecutor executor) {
        this.executor = executor;
    }

    private String getFromConfig(String jsonPointer, JsonObject config) {
        String configValue = ConfigUtils.getConfigValue(jsonPointer, config);
        if (configValue == null) {
            report.internalError("Configuration entry is missing for " + jsonPointer + " property");
        }
        return configValue;
    }

    @Override
    public Future<Void> gitAdd(Git repo, String filePattern) {
        return getWorkerExecutor().executeBlocking(() -> {
            LOG.info("Attempting to perform git add {} in {}", filePattern, repo.getRepository().getDirectory());
            try {
                repo.add().addFilepattern(filePattern).call();
            } catch (GitAPIException e) {
                throw new RuntimeException("Failed to add " + filePattern + " (" + e.getMessage() + ")");
            }
            return null;
        });
    }

    @Override
    public Future<Void> gitCommit(Git repo, String message) {
        return getWorkerExecutor().executeBlocking(() -> {
            LOG.info("Attempting to perform git commit in {}", repo.getRepository().getDirectory());
            try {
                RevCommit commit = repo.commit().setMessage(message).call();
                LOG.info("Commit successful: " + commit.getFullMessage());
            } catch (GitAPIException e) {
                throw new RuntimeException("Failed to commit (" + e.getMessage() + ")");
            }
            return null;
        });
    }

    @Override
    public Future<Object> gitStatusCheck(Git repo, Predicate<Status> statusPredicate) {
        LOG.info("Checking the status of repository {}", repo.getRepository().getDirectory());
        return getWorkerExecutor().executeBlocking(() -> {
            try {
                Status status = repo.status().call();
                return statusPredicate.test(status);
            } catch (GitAPIException e) {
                throw new RuntimeException("Failed to check the status (" + e.getMessage() + ")");
            }
        });
    }

    @Override
    public Future<Status> gitStatus(Git repo) {
        Future<Status> statusFuture = getWorkerExecutor().executeBlocking(() -> {
            try {
                return repo.status().call();
            } catch (GitAPIException e) {
                throw new RuntimeException("Failed to check the status (" + e.getMessage() + ")");
            }
        });
        return statusFuture;
    }

    @Override
    public Future<Void> gitRm(Git repo, Collection<String> files) {
        LOG.info("Attempting to remove files {} from repository {}", files, repo.getRepository().getDirectory());
        Future<Void> rmFuture = getWorkerExecutor().executeBlocking(() -> {
            try {
                RmCommand rmCommand = repo.rm();
                files.stream().forEach(f -> rmCommand.addFilepattern(f));
                rmCommand.call();
                LOG.info("Removal of files {} from repository {} finished", files, repo.getRepository().getDirectory());
                return null;
            } catch (GitAPIException e) {
                throw new RuntimeException("Failed to remove files (" + e.getMessage() + ")");
            }
        });
        return rmFuture;
    }

    @Override
    public Future<Void> submoduleUpdate(Git repository) {
        return getWorkerExecutor().executeBlocking(() -> {
            try {
                repository.submoduleUpdate().setCredentialsProvider(credentialsProvider).call();
            } catch (GitAPIException e) {
                throw new RuntimeException("Unable to perform submodule update: " + ExceptionUtils.getStackTrace(e));
            }
            return null;
        });
    }

    @Override
    public List<Future<?>> bulkSubmoduleAdd(Git superRepo, List<Map<String, JsonObject>> components) {
        List<Future<?>> futures = new ArrayList<>();
        Map<String, SubmoduleStatus> submoduleStatus;
        try {
            submoduleStatus = superRepo.submoduleStatus().call();
        } catch (GitAPIException e) {
            report.exceptionThrown(new JsonObject(), e);
            return futures;
        }
        for (Map<String, JsonObject> componentJson : components) {
            JsonObject component = componentJson.get("component");
            String cloneFolder = component.getString("directoryPath");
            Path submodulePath = Path.of(cloneFolder);
            String componentId = component.getString(F_ID);
            JsonObject domain = componentJson.get("domain");

            if (submoduleStatus.keySet().stream().anyMatch(modulePath ->
                    submodulePath.endsWith(modulePath))) {
                LOG.info("Component submodule of {} in {} already exist", componentId, cloneFolder);
                continue;
            }
            Future<?> future = getWorkerExecutor().executeBlocking(() -> {
                submoduleAddHandler(superRepo, component, domain);
                return null;
            });
            futures.add(future);
        }
        return futures;
    }

    public void submoduleAddHandler(Git superRepo, JsonObject component, JsonObject domain) {
        if (superRepo == null) {
            throw new RuntimeException("Superrepo cannot be null");
        }
        String repositoryLink = component.getString(F_REPOSITORY);
        String domainId = domain.getString(F_ID);
        String componentId = component.getString(F_ID);

        LOG.info("Adding the repository of component {}@{} with source {}", componentId, domainId, repositoryLink);

        if (repositoryLink == null) {
            report.mandatoryValueMissed(component, F_REPOSITORY);
            throw new RuntimeException("Component " + component.getString(F_ID) + " doesn't contain repository link");
        }

        String cloneFolder = component.getString("directoryPath");
        Path submodulePath = Path.of(cloneFolder);

        Repository repository = null;
        try {
            String submoduleDir = superRepo.getRepository().getDirectory().toPath().getParent().toAbsolutePath()
                    .relativize(submodulePath.toAbsolutePath())
                    .toString().replaceAll("\\\\", "/");
            // Workaround: clean existing submodule work/git dirs to avoid non-empty directory errors
            // when adding submodules that exist in other branches
            try {
                File modulesBase = new File(superRepo.getRepository().getCommonDirectory(),
                        Constants.MODULES);
                File moduleGitDir = new File(modulesBase, submoduleDir);
                File moduleWorkDir = new File(superRepo.getRepository().getWorkTree(), submoduleDir);
                if (moduleWorkDir.exists()) {
                    LOG.warn("Removing existing submodule work dir before add: {}", moduleWorkDir);
                    FileUtils.delete(moduleWorkDir,
                            FileUtils.RECURSIVE | FileUtils.RETRY);
                }
                if (moduleGitDir.exists()) {
                    LOG.warn("Removing existing submodule git dir before add: {}", moduleGitDir);
                    FileUtils.delete(moduleGitDir,
                            FileUtils.RECURSIVE | FileUtils.RETRY);
                }
            } catch (Exception cleanupEx) {
                LOG.warn("Cleanup of existing submodule dirs failed, proceeding: {}", cleanupEx.getMessage());
            }
            SubmoduleAddCommand submoduleAddCommand = new SubmoduleAddCommand(superRepo.getRepository());
            try {
                repository = submoduleAddCommand
                        .setCredentialsProvider(credentialsProvider)
                        .setURI(repositoryLink)
                        .setPath(submoduleDir)
                        .call();
            } catch (TransportException ex) {
                LOG.warn("Error while trying to add submodule {}: {}. Trying again once",
                        repositoryLink, ex.getMessage());
                repository = submoduleAddCommand
                        .setCredentialsProvider(credentialsProvider)
                        .setURI(repositoryLink)
                        .setPath(submoduleDir)
                        .call();
            }
        } catch (GitAPIException | JGitInternalException e) {
            report.exceptionThrown(new JsonObject(), e);
            throw new RuntimeException("Failed to add submodule " + repositoryLink + ": " + e.getMessage());
        }

        repository.close();
        LOG.info("Component repository of {} added to {}", componentId, cloneFolder);
    }

    @Override
    public Future<Void> submoduleAdd(Git superRepo, JsonObject component, JsonObject domain) {
        return getWorkerExecutor().executeBlocking(() -> {
            submoduleAddHandler(superRepo, component, domain);
            return null;
        });
    }

    @Override
    public void initSuperRepoHandler(String directoryPath, Promise promise) {
        LOG.info("Initializing the new repository {}", directoryPath);
        if (StringUtils.isEmpty(directoryPath)) {
            promise.fail("Repository directory path is empty");
            return;
        }
        File superRepoPath = new File(directoryPath);
        File gitFolder = new File(superRepoPath.getPath() + File.separator + ".git");
        if (gitFolder.exists()) {
            LOG.warn("Repository already exist");
            try {
                promise.complete(Git.open(gitFolder));
                return;
            } catch (IOException e) {
                report.exceptionThrown(new JsonObject(), e);
            }
        }
        superRepoPath.mkdirs();
        Git superRepository = null;
        try {
            superRepository = Git.init()
                    .setDirectory(superRepoPath)
                    .call();
        } catch (GitAPIException e) {
            report.exceptionThrown(new JsonObject(), e);
            promise.fail(e);
        }
        LOG.info("Repository {} cloned successfully", superRepoPath.getPath());
        promise.complete(superRepository);
    }

    public Git openRepositoryHandler() {
        String superRepositoryDir = getFromConfig(SUPER_REPOSITORY_DIR_POINTER, config);
        LOG.info("Attempting to open repository {}", superRepositoryDir);
        if (StringUtils.isEmpty(superRepositoryDir)) {
            throw new RuntimeException("Repository directory path is empty");
        }
        File superRepoPath = new File(superRepositoryDir);
        File gitFolder = new File(superRepoPath.getPath() + File.separator + ".git");
        if (gitFolder.exists()) {
            try {
                Git git = Git.open(gitFolder);
                return git;
            } catch (IOException e) {
                throw new RuntimeException("Failed to open repository " + superRepositoryDir + ": " + e.getMessage());
            }
        } else {
            throw new RuntimeException("Repository " + superRepositoryDir + " does not exist");
        }
    }

    @Override
    public Future<Git> openSuperrepository() {
        return getWorkerExecutor().executeBlocking(this::openRepositoryHandler);
    }

    public void submodulesCheckoutHandler(Git repository, String release, List<JsonObject> components) {
        LOG.info("Performing checkout of branches for release {} in all modules", release);
        if (repository == null) {
            throw new RuntimeException("Repository cannot be null");
        }
        if (StringUtils.isEmpty(release)) {
            throw new RuntimeException("Release name cannot be null or empty");
        }

        try (SubmoduleWalk walk = SubmoduleWalk.forIndex(repository.getRepository())) {
            while (walk.next()) {
                try (Repository module = walk.getRepository()) {
                    if (module == null) {
                        report.internalError("Module in superrepository "
                                + repository.getRepository().getDirectory() + " is null");
                        continue;
                    }
                    List<JsonObject> matchedComponents = components.stream()
                            .filter(o -> isComponentMatchingModule(module, o))
                            .collect(Collectors.toList());
                    if (matchedComponents.size() == 0) {
                        continue;
                    } else if (matchedComponents.size() > 1) {
                        report.internalError("Found more than one component for repository "
                                + module.getDirectory().toString() + ": " + matchedComponents);
                        // will not fail the promise just because of one of the repos
                        continue;
                    }
                    String branch = (String) JsonPointer.from("/details/releaseBranch")
                            .queryJson(matchedComponents.stream().findFirst().get());
                    checkout(Git.wrap(module), branch);
                } catch (GitAPIException e) {
                    report.internalError("Unable to checkout release " + release
                            + ": " + ExceptionUtils.getStackTrace(e));
                    // will not fail the promise just because of one of the repos
                    continue;
                }
                LOG.info("Checkout of release " + release + " in repository "
                        + walk.getRepository().getWorkTree().toString() + " completed.");
            }
        } catch (IOException e) {
            report.internalError("Unable to perform checkout of " + release + ": " + e.getMessage());
            throw new RuntimeException("Unable to perform checkout of " + release + ": " + e.getMessage());
        }
    }

    private Boolean isComponentMatchingModule(Repository module, JsonObject component) {
        String moduleDir = getTrailingSubpath(module.getDirectory().toPath(), 2);
        String directoryPathDir = getTrailingSubpath(Path.of(component.getString("directoryPath")), 2);
        return directoryPathDir.equals(moduleDir);
    }

    private String getTrailingSubpath(Path directoryPath, int levelsToKeep) {
        int directoryPathLength = directoryPath.getNameCount();
        return directoryPath.subpath(directoryPathLength - levelsToKeep, directoryPathLength).toString();
    }

    @Override
    public Future<Void> submodulesCheckout(Git superrepo, String release, List<JsonObject> components) {
        return getWorkerExecutor().executeBlocking(() -> {
            submodulesCheckoutHandler(superrepo, release, components);
            return null;
        });
    }

    public void checkout(Git repository, String ref) throws GitAPIException {
        LOG.info("Checking out ref " + ref + " of repository " + repository.getRepository().getDirectory());
        List<RemoteConfig> remotes = repository.remoteList().call();
        String reference = ref;
        if (!remotes.isEmpty()) {
            String remote = remotes.get(0).getName();
            reference = remote + "/" + ref;
            LOG.debug("Performing git fetch for repository {}", repository.getRepository().getDirectory());
            repository.fetch()
                    .setCredentialsProvider(credentialsProvider)
                    .call();
        }
        try {
            repository.checkout()
                    .setName(reference)
                    .setForceRefUpdate(true)
                    .setForced(true)
                    .call();
        } catch (RefNotFoundException e) {
            // In case of sha1
            LOG.debug("Reference {} was not found, trying to find local ref {} in {}", reference,
                    ref, repository.getRepository().getDirectory());
            repository.checkout()
                    .setName(ref)
                    .setForceRefUpdate(true)
                    .setForced(true)
                    .call();
        }
    }

    @Override
    public Future branchCheckout(Git repository, String branch) {
        return getWorkerExecutor().executeBlocking(() -> {
                    try {
                        checkout(repository, branch);
                        return null;
                    } catch (GitAPIException e) {
                        throw new RuntimeException("Failed to checkout branch " + branch + ": " + e.getMessage());
                    }
                })
                .onFailure(e -> report.internalError("Failed to checkout branch " + branch + ": "
                        + ExceptionUtils.getStackTrace(e)));
    }

    public Git prepareSuperRepoHandler() {
        String superRepositoryDir = getFromConfig(SUPER_REPOSITORY_DIR_POINTER, config);
        String superRepositoryUri = getFromConfig(SUPER_REPOSITORY_URL_POINTER, config);

        if (StringUtils.isEmpty(superRepositoryDir)) {
            throw new RuntimeException("Repository directory path is empty");
        }
        LOG.info("Preparing repository {}", superRepositoryDir);
        File gitFolder = Path.of(superRepositoryDir, ".git").toFile();
        Git superRepository = null;
        if (gitFolder.exists()) {
            LOG.info("Repository already exist");
            try {
                superRepository = Git.open(gitFolder);
            } catch (IOException e) {
                LOG.error("Unable to open super repository {}", gitFolder);
                throw new RuntimeException("Unable to open super repository " + gitFolder + ": " + e.getMessage());
            }
        } else {
            try {
                superRepository = superRepositoryClone(superRepositoryUri, superRepositoryDir);
            } catch (GitAPIException e) {
                LOG.warn("Clone of the super repository failed: {}", ExceptionUtils.getMessage(e));
                try {
                    superRepository = prepareLocalRepo(superRepositoryDir);
                } catch (GitAPIException ex) {
                    LOG.error("Unable to init the new super repository {}", superRepositoryDir);
                    throw new RuntimeException("Unable to init the new super repository "
                            + superRepositoryDir + ": " + ex.getMessage());
                }
            }
        }
        return superRepository;
    }

    private Git prepareLocalRepo(String superRepositoryUri) throws GitAPIException {
        LOG.warn("Preparing empty repository in {}", superRepositoryUri);

        String main = ConfigUtils.getConfigValue(ConfigProperties.DEFAULT_MAIN_BRANCH_POINTER, config);
        Git superRepository = Git.init().setDirectory(new File(superRepositoryUri)).setInitialBranch(main).call();
        superRepository.commit().setMessage("initial commit").setAllowEmpty(true).call();

        LOG.info("New repository creation in {} finished successfully", superRepositoryUri);
        return superRepository;
    }

    @Override
    public Future switchSuperRepoBranch(Git superRepository, String superRepositoryBranch) {
        return getWorkerExecutor().executeBlocking(() -> {
            try {
                checkoutSuperRepoReleaseBranch(superRepository, superRepositoryBranch);
            } catch (GitAPIException e) {
                throw new RuntimeException("Failed to checkout branch " + superRepositoryBranch
                        + ": " + e.getMessage());
            }
            return null;
        }).onFailure(e -> report.internalError("Could not checkout " + superRepositoryBranch
                + " branch of superrepository: " + ExceptionUtils.getStackTrace(e)));
    }

    @Override
    public Future<Git> prepareSuperRepository() {
        return getWorkerExecutor().executeBlocking(this::prepareSuperRepoHandler);
    }

    private void checkoutSuperRepoReleaseBranch(Git superRepository, String superRepositoryBranch)
            throws GitAPIException {
        LOG.info("Attempting to checkout branch {} in super repository", superRepositoryBranch);
        try {
            checkout(superRepository, superRepositoryBranch);
        } catch (GitAPIException e) {
            String main = ConfigUtils.getConfigValue(ConfigProperties.DEFAULT_MAIN_BRANCH_POINTER, config);
            String branch = "origin/" + main;
            if (superRepository.remoteList().call().isEmpty()) {
                branch = main;
            }
            LOG.warn("Checkout of branch {} failed ({}), creating the new branch based on {}",
                    superRepositoryBranch, e.getMessage(), branch);
            superRepository.branchCreate()
                    .setStartPoint(branch)
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                    .setName(superRepositoryBranch)
                    .setForce(true)
                    .call();
            superRepository.checkout().setName(superRepositoryBranch).call();
        }
        LOG.info("Performing submodule init for repository {}", superRepository.getRepository().getDirectory());
        superRepository.submoduleInit().call();
    }

    private Git superRepositoryClone(String uri, String directoryPath) throws GitAPIException {
        LOG.info("Cloning the repository {} to {}", uri, directoryPath);

        String main = ConfigUtils.getConfigValue(ConfigProperties.DEFAULT_MAIN_BRANCH_POINTER, config);

        Git superRepo = Git.cloneRepository()
                .setCloneSubmodules(true)
                .setURI(uri)
                .setCredentialsProvider(credentialsProvider)
                .setDirectory(new File(directoryPath))
                .setBranch(main)
                .call();
        LOG.info("Performing submodule init for repository {}", directoryPath);
        superRepo.submoduleInit().call();
        return superRepo;
    }

}
