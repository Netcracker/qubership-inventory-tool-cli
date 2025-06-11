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

package org.qubership.itool.modules.processor.tasks;

import java.util.function.Function;

import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.processor.GraphMerger;
import org.qubership.itool.modules.processor.InvalidGraphException;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

/**
 * Functionality similar to CLI flow tasks. Needed to perform some maintenance of graph integrity
 * for those who uses only {@link GraphMerger} from qubership-inventory-tool-core without flows from qubershipinventory-tool-cli.
 */
public interface GraphProcessorTask {

    /**
     * Process synchronously
     *
     * @param graph Graph to process
     * @throws InvalidGraphException If graph was considered invalid
     */
    void process(Graph graph) throws InvalidGraphException;

    /**
     * Process asynchronously.
     * <b>Default implementation is synchronous processing in caller's thread!</b>
     * Some implementing tasks may use full power of provided {@link Vertx} instance.
     *
     * @param vertx Vertx to use
     * @param graph Graph to process
     * @return A Future
     */
    default Future<Void> processAsync(Vertx vertx, Graph graph) {
        return Future.future(p -> {
            process(graph);
            p.complete();
        });
    }

    /**
     * Create a chaining {@link Function} that may be used in {@link Future#compose(Function)}
     * or {@link Future#flatMap(Function)}
     *
     * @param vertx Vertx to use
     * @param graph Graph to process
     * @return A Future
     */
    default Function<Void, Future<Void>> thenProcessAsync(Vertx vertx, Graph graph) {
        return v -> processAsync(vertx, graph);
    }

}
