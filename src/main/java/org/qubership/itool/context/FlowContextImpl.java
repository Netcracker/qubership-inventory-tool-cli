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

package org.qubership.itool.context;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graph.GraphClassifier;
import org.qubership.itool.modules.graph.GraphClassifierBuilder;
import org.qubership.itool.modules.graph.GraphDumpSupport;
import org.qubership.itool.modules.graph.GraphService;
import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;
import com.google.inject.Key;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FlowContextImpl implements FlowContext {
    private static final Logger LOG = LoggerFactory.getLogger(FlowContextImpl.class);

    private final String flowInstanceId = UUID.randomUUID().toString();

    private final Map<Class<?>, Object> resources = new HashMap<>();
    private Graph graph;
    private GraphService graphService;
    private GraphClassifier graphClassifier;
    private GraphReport report;
    private JsonObject config;
    private ClassLoader taskClassLoader;
    private Vertx vertx;
    private boolean breakRequested;
    private final Injector injector;
    private final GraphClassifierBuilder graphClassifierBuilder;

    @Inject
    public FlowContextImpl(Injector injector, Graph graph, GraphClassifierBuilder graphClassifierBuilder) {
        this.injector = injector;

        this.graph = graph;
        this.report = graph.getReport();
        this.graphClassifierBuilder = graphClassifierBuilder;
    }

    @Override
    public void initialize(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;

        if (graphService != null) {
            resources.put(GraphService.class, graphService);
        }
        if (graphClassifier != null) {
            resources.put(GraphClassifier.class, graphClassifier);
        }

    }

    @Override
    public void initialize(Object task) {
        List<Field> fields = new ArrayList<>();
        Class<?> clazz = task.getClass();
        while (clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        setAnnotatedFieldValues(task, fields);
        LOG.debug("Dependency injections for task {} complete", task);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getResource(Class<T> clazz) {
        return (T) resources.get(clazz);
    }

    @Override
    public Map<Class<?>, Object> getResources() {
        return resources;
    }

    @Override
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Override
    public void setGraphService(GraphService graphService) {
        this.graphService = graphService;
        if (graphService != null) {
            graphClassifier = graphClassifierBuilder
                .setId("flow-" + flowInstanceId)
                .setWithReport(true)
                .build();
            graphService.putGraph(graphClassifier, this.graph);
            LOG.info("[fiid={}]: Graph @{} created for {}", flowInstanceId, System.identityHashCode(graph), graphClassifier);
        } else {
            LOG.info("[fiid={}]: Graph @{} created, not attached to graph service", flowInstanceId, System.identityHashCode(graph));
        }

    }

    @Override
    public Graph getGraph() {
        return graph;
    }

    @Override
    public GraphReport getReport() {
        return report;
    }

    private void setAnnotatedFieldValues(Object obj, List<Field> fields) {
        for (Field field : fields) {
            Annotation annotation = field.getDeclaredAnnotation(Resource.class);
            if (annotation == null) {
                continue;
            }

            Class<?> fieldType = field.getType();
            // Try to get class by type from resources first
            Object resource = this.resources.get(fieldType);
            if (resource == null) {
                try {
                    Key<?> key = Key.get(field.getGenericType());
                    resource = injector.getInstance(key);
                } catch (Exception e) {
                    LOG.warn("Could not get instance for field {} of type {}: {}",
                            field.getName(), field.getGenericType(), e.getMessage());
                }
            }

            if (resource != null) {
                setFieldValue(obj, field, resource);
                LOG.trace("Field {} of {} was updated with value {}", field.getName(), obj, resource);
            } else if (field.getDeclaredAnnotation(Nullable.class) != null) {
                LOG.warn("No resource provided for " + fieldType + " in " + obj.getClass().getName());
            } else {
                throw new IllegalArgumentException("Resource not found for " + fieldType + " in " + obj.getClass().getName());
            }
        }
    }

    private void setFieldValue(Object obj, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            LOG.error("Can't initialize " + field.getType() + " in the " + obj.getClass().getName() + ". Reason: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void dumpDataToFile(File folder, String file) {
        JsonObject dump = GraphDumpSupport.dumpToJson(graph, false);

        if (folder != null && !folder.exists()) {
            folder.mkdirs();
        }
        File progressFile = new File(folder, file);
        try {
            JsonUtils.saveJson(progressFile.toPath(), dump, true);
        } catch (IOException e) {
            LOG.error("Exception when saving progress file " + progressFile, e);
        }
    }

    @Override
    public void restoreData(JsonObject dump) {
        GraphDumpSupport.restoreFromJson(graph, dump);
    }

    @Override
    public Vertx getVertx() {
        return vertx;
    }

    @Override
    public JsonObject getConfig() {
        return config;
    }

    @Override
    public ClassLoader getTaskClassLoader() {
        return taskClassLoader;
    }

    @Override
    public void setTaskClassLoader(ClassLoader taskClassLoader) {
        this.taskClassLoader = taskClassLoader;
    }

    @Override
    public void setBreakRequested(boolean breakRequested) {
        this.breakRequested = breakRequested;
    }

    @Override
    public boolean isBreakRequested() {
        return breakRequested;
    }

    @Override
    public String getFlowInstanceId() {
        return flowInstanceId;
    }

    @Override
    public GraphClassifier getGraphClassifier() {
        return graphClassifier;
    }

}
