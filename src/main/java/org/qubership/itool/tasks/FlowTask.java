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

package org.qubership.itool.tasks;

import org.qubership.itool.context.FlowContext;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.gremlin2.graph.GraphTraversal;
import org.qubership.itool.modules.gremlin2.graph.GraphTraversalSource;
import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.utils.ConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import static org.qubership.itool.utils.ConfigProperties.DISABLED_FEATURES_PROPERTY;
import static org.qubership.itool.utils.ConfigProperties.LAST_STEP_PROPERTY;
import java.io.File;
import java.time.Duration;
import java.util.*;

/**
 * Abstract base class for all flow tasks in the inventory tool.
 * Provides common functionality for task execution, progress tracking,
 * and graph operations.
 */
public abstract class FlowTask {

    protected static final Logger LOG = LoggerFactory.getLogger(FlowTask.class);

    public static final String TASK_ADDRESS_PREFIX = "task.";

    public static final String PROGRESS_PATH = "progress";


    @Resource
    protected Vertx vertx;
    @Resource
    protected FlowContext flowContext;
    @Resource
    protected GraphReport report;
    @Resource
    protected Graph graph;

    private Long executionStart;


    /**
     * Returns the features that this task supports.
     * Override this method to specify task-specific features.
     *
     * @return array of feature names supported by this task
     */
    protected String[] features() {
        return new String[0];
    }

    /**
     * Returns the logger instance for this task.
     *
     * @return the logger instance
     */
    protected Logger getLogger() {
        return LOG;
    }

    protected JsonObject config() {
        return flowContext.getConfig();
    }

    @Override
    public String toString() {
        return super.toString()
                + "[fiid="
                + (flowContext==null ? "<null>" : flowContext.getFlowInstanceId())
                + "]";
    }


    //------------------------------------------------------
    // Start and finish task in flow

    /**
     * Abstract method that must be implemented by subclasses to define the actual task logic.
     * This method is called when the task starts execution.
     *
     * @param taskPromise the promise to complete when the task finishes
     * @throws Exception if an error occurs during task execution
     */
    abstract protected void taskStart(Promise<?> taskPromise) throws Exception;

    public Future<?> startInFlow() {
        return Future.future(taskPromise -> startInFlow(taskPromise));
    }

    protected void startInFlow(Promise<?> taskPromise) {
        String taskAddress = getTaskAddress();
        String[] features = features();
        String fiid = flowContext.getFlowInstanceId();
        if (flowContext.isBreakRequested() && !Arrays.asList(features).contains("unskippable")) {
            getLogger().info("Task {} [fiid={}] is skipped", taskAddress, fiid);
            taskCompleted(taskPromise);
            return;
        }

        Set<String> disabledFeatures = new HashSet<>(Arrays.asList(
                config().getString(DISABLED_FEATURES_PROPERTY, "").split("\\s*,\\s*")));
        for (String feature : features) {
            if (disabledFeatures.contains(feature)) {
                getLogger().info("Task {} [fiid={}] ignored because feature {} is disabled",
                        taskAddress, fiid, feature);
                saveProgressIfRequired()
                        .onComplete(r -> taskCompleted(taskPromise));
                return;
            }
        }

        executionStart = System.nanoTime();
        getLogger().info("Task started: {} [fiid={}]", taskAddress, fiid);

        saveProgressIfRequired()
                .onComplete(r -> {
                    try {
                        taskStart(taskPromise);
                    } catch (Throwable e) {
                        report.internalError("Failed to execute the task '" + taskAddress
                                + "' [fiid=" + fiid + "], exception: " + ExceptionUtils.getStackTrace(e));
// XXX Usually we prefer to continue the flow on failure. Implementations may fail the promise manually if needed.
                        taskCompleted(taskPromise);
                    }
                });
    }

    protected void taskCompleted(Promise<?> taskPromise) {
        String taskAddress = getTaskAddress();

        if (executionStart != null) {
            getLogger().info("Task {} [fiid={}] finished in {}.", taskAddress,
                    flowContext.getFlowInstanceId(), Duration.ofNanos(System.nanoTime() - executionStart));
        }

        String lastStep = config().getString(LAST_STEP_PROPERTY);
        if (lastStep != null && (TASK_ADDRESS_PREFIX + lastStep).equals(taskAddress)) {
            flowContext.setBreakRequested(true);
        }

        taskPromise.tryComplete();
    }

    /**
     * Joins the futures as a composite future, handles exceptions within the futures.
     * @param futureList list of futures to be joined and handled
     * @return a future that will be completed when all the futures in the list are completed
     */
    protected Future<?> joinFuturesAndHandleResult(List<? extends Future<?>> futureList) {
        return Future.join(futureList)
                .onFailure(e -> {
                    getLogger().error("Internal errors were encountered during execution of block of futures: ");
                    futureList.stream().forEach(f -> {
                        if (f.failed()) {
                            report.internalError(ExceptionUtils.getStackTrace(f.cause()));
                        }
                    });
                })
                .onSuccess(res -> getLogger().debug("{} futures completed", futureList.size()));
    }

    private Future<Void> saveProgressIfRequired() {
        String saveProgress = config().getString(ConfigProperties.SAVE_PROGRESS_PROPERTY);
        if (saveProgressForThisTask(saveProgress)) {
            String taskName = getTaskAddress();
            getLogger().info("Save progress before execute step '{}'", taskName);
            return vertx.executeBlocking(() -> {
                        flowContext.dumpDataToFile(new File(PROGRESS_PATH), taskName + ".json");
                        return (Void) null;
                    })
                    .onFailure(e -> report.internalError("Failed to save the progress for task '"
                            + taskName + "', reason: " + ExceptionUtils.getStackTrace(e))
                    );
        }
        return Future.succeededFuture();
    }

    /* @see FlowMainVerticle#getPossibleClassNames() */
    public String getTaskAddress() {
        String address = this.getClass().getSimpleName();
        address = address.replaceFirst("(Task|Verticle)$", "");
        address = TASK_ADDRESS_PREFIX + StringUtils.uncapitalize(address);
        return address;
    }

    protected boolean saveProgressForThisTask(String saveProgress) {
        if (StringUtils.isBlank(saveProgress) || saveProgress.equals("false")) {
            return false;
        }
        if (saveProgress.equals("true")) {
            return true;
        }
        String taskNameShort =  // Assume no one overrides getTaskAddress()
                getTaskAddress().substring(TASK_ADDRESS_PREFIX.length());
        return Arrays.asList(saveProgress.split("\\s*,\\s*")).contains(taskNameShort);
    }


    //------------------------------------------------------
    // Working with the graph

    public static List<JsonObject> getComponents(Graph graph, JsonObject domain) {
        return graph.traversal().V(domain.getString(Graph.F_ID)).out().hasKey("repository", "details").toList();
    }

    public static GraphTraversal<JsonObject, JsonObject> V(Graph graph, List<String> vertexIds) {
        return new GraphTraversalSource(graph).V(vertexIds.toArray(new String[vertexIds.size()]));
    }

    protected GraphTraversal<JsonObject, JsonObject> V(final List<String> vertexIds) {
        return V(this.graph, vertexIds);
    }

    public static GraphTraversal<JsonObject, JsonObject> V(Graph graph, String... vertexIds) {
        return new GraphTraversalSource(graph).V(vertexIds);
    }

    protected GraphTraversal<JsonObject, JsonObject> V(final String... vertexIds) {
        return V(this.graph, vertexIds);
    }

    public static GraphTraversal<JsonObject, JsonObject> E(Graph graph, List<String> edgeIds) {
        return new GraphTraversalSource(graph).E(edgeIds.toArray(new String[edgeIds.size()]));
    }

    protected GraphTraversal<JsonObject, JsonObject> E(final List<String> edgeIds) {
        return E(this.graph, edgeIds);
    }

    public static GraphTraversal<JsonObject, JsonObject> E(Graph graph, String... edgeIds) {
        return new GraphTraversalSource(graph).E(edgeIds);
    }

    protected GraphTraversal<JsonObject, JsonObject> E(String... edgeIds) {
        return E(this.graph, edgeIds);
    }

}
