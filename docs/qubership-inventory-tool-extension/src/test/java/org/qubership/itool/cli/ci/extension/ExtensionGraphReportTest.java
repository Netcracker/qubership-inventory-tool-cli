package org.qubership.itool.cli.ci.extension;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.itool.di.QubershipModule;
import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.di.ApplicationContext;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import jakarta.inject.Provider;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class ExtensionGraphReportTest {
    
    private ApplicationContext context;
    
    @BeforeEach
    void setUp() {
        // Create a test configuration
        JsonObject config = new JsonObject()
            .put("testKey", "testValue");
            
        // Create application context with our extension module
        context = new ApplicationContext(Vertx.vertx(), config, new Module[] {
                Modules.override(new QubershipModule(Vertx.vertx()))
                        .with(new ExtensionModule()) 
                    });
    }

    @Test
    void testReportFunctionality() {
        // Get report instance from application context
        GraphReport report = context.getInstance(GraphReport.class);

        // Add some test data
        JsonObject record = new JsonObject()
            .put("id", "test-1")
            .put("name", "Test Record");
        report.addRecord(record);

        // Verify records
        JsonArray records = report.dumpRecords(true);
        assertFalse(records.isEmpty(), "Report should contain records");

        // Verify extended functionality
        boolean hasExtendedData = records.stream()
            .map(obj -> (JsonObject) obj)
            .allMatch(r -> r.containsKey("extended"));
        assertTrue(hasExtendedData, "Report should contain extended data");
        
        // Verify record content
        JsonObject firstRecord = records.getJsonObject(0);
        assertEquals("test-1", firstRecord.getString("id"), "Record should have correct id");
        assertEquals("Test Record", firstRecord.getString("name"), "Record should have correct name");
        assertTrue(firstRecord.getBoolean("extended"), "Record should have extended flag");
    }
} 