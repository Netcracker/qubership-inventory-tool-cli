package org.qubership.itool.di;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.itool.context.FlowContext;
import org.qubership.itool.modules.graph.GraphReportFactory;
import org.qubership.itool.modules.graph.DefaultGraphReportFactory;
import org.qubership.itool.modules.graph.GraphFactory;
import org.qubership.itool.modules.graph.DefaultGraphFactory;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.processor.GraphMerger;
import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.modules.report.GraphReportImpl;
import com.google.inject.Module;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class ApplicationContextTest {

    @Mock private GraphMerger mockGraphMerger;
    @Mock private FlowContext mockFlowContext;
    @Mock private GraphReportFactory mockGraphReportFactory;

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
        Module testModule = TestModule.createOverrideModule(vertx, mockGraphMerger, mockFlowContext, mockGraphReportFactory);

        // Create application context with test module
        ApplicationContext context = new ApplicationContext(vertx, config, new Module[] {testModule});

        // Verify that we get our mock instances
        GraphMerger graphMerger = context.getInstance(GraphMerger.class);
        FlowContext flowContext = context.getInstance(FlowContext.class);
        GraphReportFactory graphReportFactory = context.getInstance(GraphReportFactory.class);

        assertSame(mockGraphMerger, graphMerger, "Should get mock GraphMerger");
        assertSame(mockFlowContext, flowContext, "Should get mock FlowContext");
        assertSame(mockGraphReportFactory, graphReportFactory, "Should get mock GraphReportFactory");

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

        // Create a factory that produces our custom report
        class LoggingGraphReportFactory implements GraphReportFactory {
            @Override
            public GraphReport createGraphReport() {
                return new LoggingGraphReport();
            }
        }

        // Create test module with custom factory
        Module testModule = TestModule.createOverrideModule(
            vertx, 
            mockGraphMerger, 
            mockFlowContext, 
            new LoggingGraphReportFactory()
        );

        // Create application context with test module
        ApplicationContext context = new ApplicationContext(vertx, config, new Module[] {testModule});

        // Create a graph and verify that our custom report is used
        Graph graph = context.getInstance(GraphFactory.class).createGraph();

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
        GraphReportFactory graphReportFactory = context.getInstance(GraphReportFactory.class);

        assertNotNull(graphMerger, "Should get real GraphMerger");
        assertNotNull(flowContext, "Should get real FlowContext");
        assertNotNull(graphReportFactory, "Should get real GraphReportFactory");
        assertNotSame(mockGraphMerger, graphMerger, "Should not get mock GraphMerger");
        assertNotSame(mockFlowContext, flowContext, "Should not get mock FlowContext");
        assertNotSame(mockGraphReportFactory, graphReportFactory, "Should not get mock GraphReportFactory");

        testContext.completeNow();
    }
} 