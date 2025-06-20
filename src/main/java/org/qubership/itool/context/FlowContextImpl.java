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
import io.vertx.ext.web.client.WebClient;

import org.qubership.itool.modules.confluence.ConfluenceClient;
import org.qubership.itool.modules.confluence.ConfluenceClientBuilder;
import org.qubership.itool.modules.diagram.DiagramService;
import org.qubership.itool.modules.diagram.DiagramServiceImpl;
import org.qubership.itool.modules.diagram.providers.DomainDiagramProvider;
import org.qubership.itool.modules.diagram.providers.GeneralDomainsDiagramProvider;
import org.qubership.itool.modules.diagram.providers.InfrastructureDiagramProvider;
import org.qubership.itool.modules.diagram.providers.MicroserviceDiagramProvider;
import org.qubership.itool.modules.diagram.providers.QueueDiagramProvider;
import org.qubership.itool.modules.git.GitAdapter;
import org.qubership.itool.modules.git.GitAdapterBuilder;
import org.qubership.itool.modules.git.GitFileRetriever;
import org.qubership.itool.modules.git.GitFileRetrieverBuilder;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.graph.GraphClassifier;
import org.qubership.itool.modules.graph.GraphClassifierBuilderImpl;
import org.qubership.itool.modules.graph.GraphDumpSupport;
import org.qubership.itool.modules.graph.GraphService;
import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.modules.template.TemplateService;
import org.qubership.itool.modules.template.TemplateServiceImpl;
import org.qubership.itool.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.qubership.itool.modules.diagram.providers.DiagramProvider.*;
import org.qubership.itool.modules.processor.MergerApi;

public class FlowContextImpl implements FlowContext {
    private static final Logger LOG = LoggerFactory.getLogger(FlowContextImpl.class);

    private TemplateService templateService;
    private DiagramService diagramService;
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
    private final Provider<Graph> graphProvider;
    private final Provider<GraphReport> graphReportProvider;
    private final Provider<MergerApi> graphMergerProvider;

    private final Map<Class<?>, Provider<?>> providerRegistry = new HashMap<>();

    @Inject
    public FlowContextImpl(Provider<Graph> graphProvider, Provider<GraphReport> graphReportProvider, Provider<MergerApi> graphMergerProvider) {
        this.graphProvider = graphProvider;
        this.graphReportProvider = graphReportProvider;
        this.graphMergerProvider = graphMergerProvider;

        this.graph = graphProvider.get();
        this.report = graph.getReport();
    }

    @Override
    public void initialize(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;

        Properties diagramProperties = new Properties();
        diagramProperties.setProperty(SKINPARAM_BACKGROUND_COLOR_DEFAULT_DOMAIN, "Gold");
        diagramProperties.setProperty(SKINPARAM_BACKGROUND_COLOR_DEFAULT_COMPONENT, "Yellow");
        diagramProperties.setProperty(SKINPARAM_BACKGROUND_COLOR_DATABASE, "DeepSkyBlue");
        diagramProperties.setProperty(SKINPARAM_BACKGROUND_COLOR_QUEUE, "GreenYellow");
        diagramProperties.setProperty(SKINPARAM_BACKGROUND_COLOR_CACHING, "Orchid");

        this.diagramService = new DiagramServiceImpl(this.getGraph(), diagramProperties);
        this.diagramService.register(new MicroserviceDiagramProvider());
        this.diagramService.register(new DomainDiagramProvider());
        this.diagramService.register(new GeneralDomainsDiagramProvider());
        this.diagramService.register(new InfrastructureDiagramProvider());
        this.diagramService.register(new QueueDiagramProvider());
        this.templateService = new TemplateServiceImpl(this.diagramService, config);

        WebClient client = WebClient.create(vertx);
        GitAdapter gitAdapter = GitAdapterBuilder.create(vertx, report, config);
        ConfluenceClient confluenceClient = ConfluenceClientBuilder.create(vertx, client, config);
        GitFileRetriever gitFileRetriever = GitFileRetrieverBuilder.create(gitAdapter, config, vertx, report);

        this.resources.put(FlowContext.class, this);
        this.resources.put(Vertx.class, vertx);
        this.resources.put(Graph.class, this.graph);
        this.resources.put(GraphReport.class, this.report);
        this.resources.put(WebClient.class, client);
        this.resources.put(ConfluenceClient.class, confluenceClient);
        this.resources.put(DiagramService.class, this.diagramService);
        this.resources.put(TemplateService.class, this.templateService);
        this.resources.put(GitAdapter.class, gitAdapter);
        this.resources.put(GitFileRetriever.class, gitFileRetriever);

        if (graphService != null) {
            resources.put(GraphService.class, graphService);
        }
        if (graphClassifier != null) {
            resources.put(GraphClassifier.class, graphClassifier);
        }

        // Register providers
        registerProvider(Graph.class, this.graphProvider);
        registerProvider(GraphReport.class, this.graphReportProvider);
        registerProvider(MergerApi.class, this.graphMergerProvider);
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
            graphClassifier = new GraphClassifierBuilderImpl()
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
            Object resource = null;

            // Handle Provider<T> types
            if (Provider.class.isAssignableFrom(fieldType)) {
                // Extract the generic type parameter
                try {
                    ParameterizedType paramType = 
                        (ParameterizedType) field.getGenericType();
                    Class<?> providerType = (Class<?>) paramType.getActualTypeArguments()[0];
                    
                    // Get provider from registry
                    resource = getProvider(providerType);
                } catch (Exception e) {
                    LOG.warn("Could not determine provider type for field {} in {}", field.getName(), obj.getClass().getName());
                }
            } else {
                // Handle regular types
                resource = this.resources.get(fieldType);
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

    /**
     * Provider registration helper method
     * @param type - type of the provider
     * @param provider - provider for the given type
     */
    private <T> void registerProvider(Class<T> type, Provider<T> provider) {
        providerRegistry.put(type, provider);
    }

    /**
     * Get provider for the given type on demand
     * @param type - type of the provider
     * @return provider for the given type
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> Provider<T> getProvider(Class<T> type) {
        return (Provider<T>) providerRegistry.get(type);
    }
}
