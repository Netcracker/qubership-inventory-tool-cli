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

package org.qubership.itool.utils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;

public class FutureUtils {

    private static final Logger LOG = LoggerFactory.getLogger(FutureUtils.class);


    /** Block until completion, return raw result or rethrow resulting exception.
     * In case of interrupt, returns {@code null} and sets interrupted status of the current thread.
     * @param <T> Future type
     * @param future Future instance
     * @return Future result
     */
    public static <T> T blockForResultOrException(Future<T> future) {
        return getResultOrException(blockForAsyncResult(future));
    }

    /** Block until completion or timeout, return raw result or rethrow resulting exception.
     * In case of interrupt, returns {@code null} and sets interrupted status of the current thread.
     * In case of timeout, returns {@code null}.
     *
     * @param <T> Result type
     * @param future Future to wait for
     * @param timeout Timeout value
     * @param timeUnit Timeout unit
     * @return Returns result, or null in case of timeout, or rethrows failure.
     */
    public static <T> T blockForResultOrException(Future<T> future, long timeout, TimeUnit timeUnit) {
        return getResultOrException(blockForAsyncResult(future, timeout, timeUnit));
    }

    /** Block until completion, return {@link AsyncResult} indicating either success or failure.
     * In case of interrupt, returns {@code null} and sets interrupted status of the current thread.
     * @param <T> Future type
     * @param future Future instance
     * @return Future result
     */
    public static <T> AsyncResult<T> blockForAsyncResult(Future<T> future) {
        if (future.isComplete()) {
            return future;
        }

        BlockingQueue<AsyncResult<T>> resultPipe = new ArrayBlockingQueue<>(1);
        future.onComplete((AsyncResult<T> ar) -> resultPipe.offer(ar));

        try {
            return resultPipe.take();
        } catch (InterruptedException e) {
            LOG.error("Interrupted", e);
            Thread.currentThread().interrupt();
            return Future.failedFuture(e);
        }
    }

    /** Block until completion or timeout, return {@link AsyncResult}. In case of timeout, returns {@code null}.
     * In case of interrupt, returns {@code null} and sets interrupted status of the current thread.
     * @param <T> Future type
     * @param future Future instance
     * @param timeout Timeout value
     * @param timeUnit Timeout unit
     * @return Future result
     */
    public static <T> AsyncResult<T> blockForAsyncResult(Future<T> future, long timeout, TimeUnit timeUnit) {
        if (future.isComplete()) {
            return future;
        }

        BlockingQueue<AsyncResult<T>> resultPipe = new ArrayBlockingQueue<>(1);
        future.onComplete((AsyncResult<T> ar) -> resultPipe.offer(ar));

        try {
            return resultPipe.poll(timeout, timeUnit);
        } catch (InterruptedException e) {
            LOG.error("Interrupted", e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public static <T> T getResultOrException(AsyncResult<T> ar) {
        if (ar == null) {
            return null;
        }
        if (ar.failed()) {
            Throwable cause = ar.cause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException)cause;
            }
            throw new RuntimeException(cause);
        }
        return ar.result();
    }

    public static <T> Optional<T> anyResult(List<Future<T>> futuresList) {
        return futuresList.stream()
            .filter(Future::succeeded)
            .findAny()
            .map(Future::result);
    }

    public static Optional<Throwable> anyCause(@SuppressWarnings("rawtypes") List<Future> futuresList) {
        return futuresList.stream()
            .filter(Future::failed)
            .findAny()
            .map(Future::cause);
    }

    /** Get a {@link Future} that will complete with <b>any successful</b> result
     * (null and Throwable results are NOT supported!) from given list of futures,
     * or with <b>any failure</b> in case when <b>all</b> the source futures have failed.
     * For an empty input list, returns an empty succeeded Future.
     *
     * @param <T> Futures type
     * @param futures Futures
     * @return Composite future
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Future<T> anyFuture(List<Future<T>> futures) {
        if (futures.isEmpty()) {
            return Future.succeededFuture();
        }
        if (futures.size() == 1) {
            return futures.get(0);
        }
        return CompositeFuture.any((List)futures)
            .flatMap(cf -> {
                if (cf.succeeded()) {
                    return Future.succeededFuture(anyResult(futures).get());
                } else {
                    List<Future> list = (List)futures;
                    return Future.failedFuture(anyCause(list).get());
                }
            });
    }

    /** Invoke a group of recursive tasks in specified FJP thread pool and join all them.
     * This utility method works with JRE {@link ForkJoinPool} tasks rather
     * than VertX {@link Future}s.
     * If either subtask throws an exception, this method results in
     * {@link RuntimeException} or {@link Error} thrown.
     *
     * @param <T> Return type of tasks
     * @param tasks tasks
     * @param pool Pool to run the tasks in
     * @return List of task results, with order preserved
     */
    public static <T> List<T> invokeAndJoin(List<RecursiveTask<T>> tasks, ForkJoinPool pool) {
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }

        @SuppressWarnings("serial")
        RecursiveTask<List<T>> composite = new RecursiveTask<>() {
            @Override
            protected List<T> compute() {
                for (RecursiveTask<?> subtask: tasks) {
                    subtask.fork();
                }
                return tasks.stream()
                    .map(RecursiveTask::join)
                    .collect(Collectors.toList());
            }
        };

        return pool.invoke(composite);
    }

    public static <T> List<T> invokeAndJoin(List<RecursiveTask<T>> tasks) {
        return invokeAndJoin(tasks, ForkJoinPool.commonPool());
    }

}
