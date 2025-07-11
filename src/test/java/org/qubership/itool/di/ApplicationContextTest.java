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
package org.qubership.itool.di;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jakarta.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.itool.context.FlowContext;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.processor.GraphMerger;
import org.qubership.itool.modules.processor.tasks.GraphProcessorTask;
import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.modules.report.GraphReportImpl;

import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class ApplicationContextTest {

    @Mock private GraphMerger mockGraphMerger;
    @Mock private FlowContext mockFlowContext;
    @Mock private GraphReport mockGraphReport;

    private Vertx vertx;
    private JsonObject config;

    @BeforeEach
    void setUp(VertxTestContext testContext) {
        vertx = Vertx.vertx();
        config = new JsonObject().put("testKey", "testValue");
        testContext.completeNow();
    }

    @Test
    void testOverrideServices(VertxTestContext testContext) {
        // Create test module with mocks
        Module testModule = TestModule.createOverrideModule(vertx, mockGraphMerger, mockFlowContext, mockGraphReport);

        // Create application context with test module
        ApplicationContext context = new ApplicationContext(vertx, config, new Module[] {testModule});

        // Verify that we get our mock instances
        GraphMerger graphMerger = context.getInstance(GraphMerger.class);
        FlowContext flowContext = context.getInstance(FlowContext.class);
        GraphReport graphReport = context.getInstance(GraphReport.class);

        assertSame(mockGraphMerger, graphMerger, "Should get mock GraphMerger");
        assertSame(mockFlowContext, flowContext, "Should get mock FlowContext");
        assertSame(mockGraphReport, graphReport, "Should get mock GraphReport");

        testContext.completeNow();
    }

    @Test
    void testCustomGraphReport(VertxTestContext testContext) {
        // Create a custom GraphReport that adds logging
        class LoggingGraphReport extends GraphReportImpl {
            @Override
            public void addRecord(JsonObject record) {
                // Add a prefix to the message to indicate it was processed by our custom report
                String originalMessage = record.getString("message", "");
                record.put("message", "[LOGGED] " + originalMessage);
                super.addRecord(record);
            }
        }

        // Create test module with custom factory
        Module testModule = TestModule.createOverrideModule(
            vertx,
            mockGraphMerger,
            mockFlowContext,
            new LoggingGraphReport()
        );

        // Create application context with test module
        ApplicationContext context = new ApplicationContext(vertx, config, new Module[] {testModule});

        // Create a graph and verify that our custom report is used
        Graph graph = context.getInstance(Graph.class);

        // Add a test record
        graph.getReport().addRecord(new JsonObject()
            .put("type", "INFO")
            .put("message", "Test message"));

        // Verify that the report contains our modified record
        JsonArray records = graph.getReport().dumpRecords(false);
        assertTrue(records.stream()
            .anyMatch(record -> ((JsonObject)record).getString("message").equals("[LOGGED] Test message")),
            "Report should contain our modified record");

        testContext.completeNow();
    }

    @Test
    void testDefaultContext(VertxTestContext testContext) {
        // Create default context
        ApplicationContext context = ApplicationContext.createDefault();

        // Verify that we get real implementations
        GraphMerger graphMerger = context.getInstance(GraphMerger.class);
        FlowContext flowContext = context.getInstance(FlowContext.class);
        GraphReport graphReport = context.getInstance(GraphReport.class);

        assertNotNull(graphMerger, "Should get real GraphMerger");
        assertNotNull(flowContext, "Should get real FlowContext");
        assertNotNull(graphReport, "Should get real GraphReport");
        assertNotSame(mockGraphMerger, graphMerger, "Should not get mock GraphMerger");
        assertNotSame(mockFlowContext, flowContext, "Should not get mock FlowContext");
        assertNotSame(mockGraphReport, graphReport, "Should not get mock GraphReport");

        testContext.completeNow();
    }

    @Test
    void testFinalizationTasks(VertxTestContext testContext) {
        // Create default context
        ApplicationContext context = ApplicationContext.createDefault();

        Provider<List<GraphProcessorTask>> finalizationTasksProvider = context.getInjector().getProvider(
            Key.get(new TypeLiteral<List<GraphProcessorTask>>() {}, Names.named("finalization.tasks"))
        );

        List<GraphProcessorTask> finalizationTasks = finalizationTasksProvider.get();
        List<GraphProcessorTask> finalizationTasks2 = finalizationTasksProvider.get();
        assertFalse(finalizationTasks.get(0)==finalizationTasks2.get(0), "Should have different instances");

        testContext.completeNow();
    }
}
