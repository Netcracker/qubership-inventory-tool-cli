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

package org.qubership.itool.tasks.dependency;

import javax.annotation.Resource;

import org.qubership.itool.modules.processor.tasks.CreateTransitiveHttpDependenciesTask;
import org.qubership.itool.modules.processor.tasks.CreateTransitiveQueueDependenciesTask;
import org.qubership.itool.modules.processor.tasks.RecreateDomainsStructureTask;
import org.qubership.itool.modules.report.GraphReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.qubership.itool.tasks.FlowTask;

import io.vertx.core.Promise;
import jakarta.inject.Provider;

/**
 * Add more edges after {@link SetEdgesBetweenComponentsVerticle} and assembly
 */
public class SetTransitiveEdgesBetweenComponentsTask extends FlowTask {

    @Resource
    Provider<GraphReport> graphReportProvider;

    @Resource
    Provider<CreateTransitiveQueueDependenciesTask> createTransitiveQueueDependenciesTaskProvider;

    @Resource
    Provider<CreateTransitiveHttpDependenciesTask> createTransitiveHttpDependenciesTaskProvider;

    @Resource
    Provider<RecreateDomainsStructureTask> recreateDomainsStructureTaskProvider;

    protected Logger LOG = LoggerFactory.getLogger(SetTransitiveEdgesBetweenComponentsTask.class);

    @Override
    protected void taskStart(Promise<?> taskPromise) throws Exception {
        // For flows including graph merging, a similar sequence of tasks is performed by GraphMerger
        createTransitiveQueueDependenciesTaskProvider.get().processAsync(vertx, graph)
        .compose(v -> createTransitiveHttpDependenciesTaskProvider.get().processAsync(vertx, graph))
        .compose(v -> recreateDomainsStructureTaskProvider.get().processAsync(vertx, graph))
        .onComplete(res -> taskCompleted(taskPromise));
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
