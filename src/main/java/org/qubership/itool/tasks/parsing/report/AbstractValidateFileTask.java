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

package org.qubership.itool.tasks.parsing.report;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import io.vertx.core.json.JsonObject;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.tasks.AbstractAggregationTaskVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import static org.qubership.itool.modules.graph.Graph.F_ID;

public abstract class AbstractValidateFileTask extends AbstractAggregationTaskVerticle {
    protected Logger LOGGER = LoggerFactory.getLogger(AbstractValidateFileTask.class);


    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    protected void taskStart(Promise<?> taskPromise) {
        Integer coresCount = CpuCoreSensor.availableProcessors();
        LOG.debug("Detected {} CPU cores, using all of them", coresCount);
        WorkerExecutor executor = vertx.createSharedWorkerExecutor("validate-file-worker-pool"
                , coresCount
                , 60
                , TimeUnit.SECONDS);

        BiFunction<Graph, JsonObject, List<JsonObject>> componentExtractor = AbstractAggregationTaskVerticle::getMavenDependencyComponents;
        @SuppressWarnings("rawtypes")
        List<Future> futures = processGraph(this::aggregateDomainData, c -> processDependencyTree(c, executor), componentExtractor);
        completeCompositeTask(futures, taskPromise);
    }

    @SuppressWarnings("rawtypes")
    private List<Future> aggregateDomainData(JsonObject jsonObject) {
        Future future = Future.succeededFuture();
        return Collections.singletonList(future);
    }

    private List<Future> processDependencyTree(JsonObject component, WorkerExecutor executor) {
        LOG.debug("{}: Scheduling blocking execution of maven dependencies import to the graph", component.getString(F_ID));
        Future blockingFuture = Future.future(promise -> executor.executeBlocking(processDependencies(component), false, promise));
        return Collections.singletonList(blockingFuture);
    }

    private Handler<Promise<Object>> processDependencies(JsonObject component) {
        return p -> {
            long executionStart = System.currentTimeMillis();
            String fileContents = (String) graph.traversal().V(component.getString("id")).out()
                    .glob(getFilePattern()).value("content").next();
            validateFile(component, fileContents);
            LOG.debug("{}: Dependency import finished in {}ms", component.getString(F_ID),
                    System.currentTimeMillis() - executionStart);
            p.complete();
        };
    }
    protected abstract String getFilePattern();

    protected abstract void validateFile(JsonObject component, String fileContents);

}
