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

package org.qubership.itool.cli;

import com.google.common.reflect.ClassPath;
import org.qubership.itool.context.FlowContext;
import org.qubership.itool.tasks.FlowTask;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.qubership.itool.utils.FSUtils;
import org.qubership.itool.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.qubership.itool.utils.ConfigProperties.START_STEP_PROPERTY;
import java.io.*;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;


public abstract class FlowMainVerticle extends AbstractVerticle {

    protected static final Logger LOG = LoggerFactory.getLogger(FlowMainVerticle.class);

    protected Logger getLogger() {
        return LOG;
    }

    protected final AtomicReference<String> deploymentIdHolder = new AtomicReference<>();
    protected final Promise<?> flowPromise = Promise.promise();

    @Resource
    protected FlowContext flowContext;
    protected long executionStart;

    private final Handler<Throwable> TERMINATOR = this::terminateFlow;

    public JsonObject config() {
        return flowContext.getConfig();
    }


    //------------------------------------------------------
    // Deploy and run flow

    /** Initialize, deploy this verticle, deploy tasks and run a flow. This method shall be called
     * not more than once for an instance.
     *
     * @param flowContext A context for this flow
     * @return A Future for this flow that will either succeed or fail when the flow ends.
     */
    public Future<?> deployAndRunFlow(FlowContext flowContext) {
        flowContext.initialize(this);

        DeploymentOptions options = new DeploymentOptions()
            .setThreadingModel(ThreadingModel.WORKER)
            .setConfig(flowContext.getConfig());
        deployThisVerticle(flowContext.getVertx(), options)
            .onSuccess(depId -> deploymentIdHolder.set(depId))
            .onFailure(TERMINATOR);

        return flowPromise.future();
    }

    /* Undeploy this flow verticle */
    public Future<?> undeploy() {
        String deploymentId = deploymentIdHolder.getAndSet(null);
        if (deploymentId == null) {
            return Future.succeededFuture();
        }
        return vertx.undeploy(deploymentId);
    }

    /* Deploy this verticle into VertX, causing method {@link #start()} to run in it.
     * The flow will be assembled and started there asynchronously.
     *
     * @return A Future that will indicate deployment result and contain deployment id on success
     */
    protected Future<String> deployThisVerticle(Vertx vertx, DeploymentOptions options) {
        return vertx.deployVerticle(this, options);
    }

    // Called by VertX. Do not call it manually!
    // Any exception thrown here will be treated by VertX as failure of deployment.
    public void start() throws Exception {

        executionStart = System.nanoTime();
        List<String> flowSequence;
        try {
            flowSequence = getFlowSequence();
        } catch (Exception e) {
            getLogger().error("Error when loading flow sequence", e);
            terminateFlow(e);
            return;
        }
        if (CollectionUtils.isEmpty(flowSequence)) {
            terminateFlow("No flow defined for " + getClass());
        }

        String startStep = config().getString(START_STEP_PROPERTY);
        if (StringUtils.isBlank(startStep)) {
            getLogger().info("========== Starting a flow: fiid={}", flowContext.getFlowInstanceId());
            startStep = flowSequence.get(0);
        } else {
            getLogger().info("========== Starting a flow from '{}': fiid={}", startStep,
                    flowContext.getFlowInstanceId());
            JsonObject dump = null;
            try {
                dump = JsonUtils.readJsonFile("progress/task." + startStep + ".json");
            } catch (IOException /* | DecodeException */ e) {
                getLogger().error("Can't restore progress file for '" + startStep + "'", e);
                terminateFlow(e);
            }
            if (dump != null) {
                flowContext.restoreData(dump);
            } else {
                terminateFlow("Can't restore progress file for '" + startStep + "'");
            }
        }

        if (! flowPromise.future().failed()) {
            deployAndRunTaskSequence(flowSequence, startStep);
        }
    }

    protected void finishFlow() {
        getLogger().info("========== Flow execution [fiid={}] completed in {}",
                flowContext.getFlowInstanceId(),
                Duration.ofNanos(System.nanoTime() - executionStart));
        flowPromise.tryComplete();
    }

    protected void terminateFlow(String message) {
        getLogger().info("========== Flow execution [fiid={}] failed: {}",
                flowContext.getFlowInstanceId(), message);
        flowPromise.tryFail(message);
    }

    protected void terminateFlow(Throwable e) {
        getLogger().error("========== Flow execution [fiid=" + flowContext.getFlowInstanceId() + "] failed", e);
        flowPromise.tryFail(e);
    }


    //------------------------------------------------------
    // Build flow sequence

    protected abstract List<String> getFlowSequence() throws Exception;

    protected List<String> loadFlowSequence(String url) throws IOException {
        try (InputStream in = FSUtils.openUrlStream(getClass(), url)) {
            LineNumberReader lnr = new LineNumberReader(new InputStreamReader(in, JsonUtils.UTF_8));
            return lnr.lines()
                    .map(line -> line.replaceFirst("(#|--|//).*$", "").trim())
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toList());
        }
    }


    //------------------------------------------------------
    // Generate flow tasks

    /*
     * Create and deploy task sequence. Currently, tasks are not verticles, so deployment
     * process is quite lightweight.
     */
    protected void deployAndRunTaskSequence(List<String> flowSequence, String startStep) throws Exception {

        Map<String, Class<? extends FlowTask>> taskClasses = getTaskClasses(flowSequence);
        List<FlowTask> taskInstances = new ArrayList<>();

        boolean skip = true;
        for (String taskName: flowSequence) {
            // Skip everything before the first step
            if (skip) {
                if (taskName.equals(startStep)) {
                    skip = false;
                } else {
                    continue;
                }
            }

            getLogger().debug("Creating task: {}", taskName);
            taskInstances.add(instantiateTask(taskClasses.get(taskName)));
        }
        if (skip) {
            terminateFlow("Step '" + startStep + "' not found");
            return;
        }

        // We are in some VertX thread where FlowMainVerticle.start() was invoked by VertX. Let's proceed right here.
        Future<?> chainReaction = Future.succeededFuture();
        for (FlowTask taskInstance: taskInstances) {
            chainReaction = chainReaction.compose(r -> taskInstance.startInFlow());
        }
        chainReaction
            .onFailure(TERMINATOR)
            .onSuccess(r -> finishFlow());
    }


    private FlowTask instantiateTask(Class<? extends FlowTask> clazz) throws Exception {
        FlowTask taskInstance = clazz.getDeclaredConstructor().newInstance();
        flowContext.initialize(taskInstance);
        return taskInstance;
    }

    protected Map<String, Class<? extends FlowTask>> getTaskClasses(Collection<String> taskNames)
            throws IOException, ClassNotFoundException {

        Map<String, Collection<String>> taskToPossibleNames = taskNames.stream()
                .distinct()
                .collect(Collectors.toMap(Function.identity(), this::getPossibleClassNames));
        Set<String> simpleNames = taskToPossibleNames.entrySet().stream()
                .flatMap(e -> e.getValue().stream())
                .collect(Collectors.toSet());

        // Map<shortName to Class>
        Map<String, Class<? extends FlowTask>> classes = new HashMap<>();
        ClassLoader taskClassLoader = flowContext.getTaskClassLoader();

        for (ClassPath.ClassInfo info: ClassPath.from(taskClassLoader).getTopLevelClasses()) {
            String shortName = info.getSimpleName();
            if (! simpleNames.contains(shortName)) {
                continue;
            }
            Class<?> clazz = Class.forName(info.getName(), false, taskClassLoader);
            if (Modifier.isAbstract(clazz.getModifiers()) || ! FlowTask.class.isAssignableFrom(clazz)) {
                continue;
            }
            getLogger().debug("Java task found: {}", clazz);
            classes.put(shortName, clazz.asSubclass(FlowTask.class));
        }
        // XXX Here we may add lookup for tasks packaged as .java files inside
        // JavaAppContextVerticleFactory.getCustomTaskPath()
        // XXX If we ever get other task factories, poll them here

        // Map<taskName to class>
        Map<String, Class<? extends FlowTask>> result = new LinkedHashMap<>();
        Set<String> missedTasks = new LinkedHashSet<>();
        for (Map.Entry<String, Collection<String>> e: taskToPossibleNames.entrySet()) {
            Optional<Class<? extends FlowTask>> clazz = e.getValue().stream()
                    .filter(simpleName -> classes.containsKey(simpleName))
                    .findAny()
                    .map(simpleName -> classes.get(simpleName));
            if (clazz.isPresent()) {
                result.put(e.getKey(), clazz.get());
            } else {
                missedTasks.add(e.getKey());
            }
        }

        if (!missedTasks.isEmpty()) {
            throw new IllegalStateException(
                    "No implementations found for the following tasks of the flow: " + missedTasks);
        }
        return result;
    }

    /* @see FlowTask#getTaskAddress() */
    protected Collection<String> getPossibleClassNames(String taskName) {
        String capitalize = StringUtils.capitalize(taskName);
        return Arrays.asList(
                capitalize + "Verticle",    // Old naming
                capitalize + "Task"         // New naming
        );
    }

}
