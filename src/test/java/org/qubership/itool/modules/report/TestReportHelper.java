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

package org.qubership.itool.modules.report;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.*;
import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.modules.report.GraphReportImpl;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestReportHelper {

    private GraphReport graphReport;

    @BeforeAll
    public void setup() {
        graphReport = new GraphReportImpl();
    }

    @BeforeEach
    public void cleanup() {
        graphReport.clear();
    }

    @Test
    public void testMessage() {
        graphReport.addMessage("TEST", createSourceComponent("test:1"), "test_message");

        JsonArray report = graphReport.dumpRecords(false);
        JsonObject record = report.getJsonObject(0);
        System.out.println(record);

        Assertions.assertEquals(1, report.size());
        Assertions.assertEquals("TEST", record.getString("type"));
        Assertions.assertEquals("test_message", record.getString("message"));
        Assertions.assertEquals("test:1", record.getString("component"));
    }

    @Test
    public void testMandataoryValueMissed() {
        graphReport.mandatoryValueMissed(createSourceComponent("test:1"), "title");

        JsonArray report = graphReport.dumpRecords(false);
        JsonObject record = report.getJsonObject(0);
        System.out.println(record);

        Assertions.assertEquals(1, report.size());
        Assertions.assertEquals("test:1", record.getString("component"));
        Assertions.assertEquals("Mandatory property missed. Property: title", record.getString("message"));
    }

    @Test
    public void testReferenceNotFound() {
        graphReport.referenceNotFound(createSourceComponent("test:1"), "12345");

        JsonArray report = graphReport.dumpRecords(false);
        JsonObject record = report.getJsonObject(0);
        System.out.println(record);

        Assertions.assertEquals(1, report.size());
        Assertions.assertEquals("test:1", record.getString("component"));
        Assertions.assertEquals("Reference was not found. Reference: 12345", record.getString("message"));
    }

    @Test
    public void testConfigurationFileNotFound() {
        graphReport.configurationFileNotFound(createSourceComponent("test:1"), "inventory.md");

        JsonArray report = graphReport.dumpRecords(false);
        JsonObject record = report.getJsonObject(0);
        System.out.println(record);

        Assertions.assertEquals(1, report.size());
        Assertions.assertEquals("test:1", record.getString("component"));
        Assertions.assertEquals("Configuration file was not found. File: inventory.md", record.getString("message"));
    }

    @Test
    public void testComponentDuplicated() {
        graphReport.componentDuplicated(
            createSourceComponent("test:1").put("name", "test_1"),
            createSourceComponent("test:1").put("name", "test_2")
        );

        JsonArray report = graphReport.dumpRecords(false);
        JsonObject record = report.getJsonObject(0);
        System.out.println(record);

        Assertions.assertEquals(1, report.size());
        Assertions.assertEquals("test:1", record.getString("component"));
        Assertions.assertEquals("Component duplicated. Id: test:1", record.getString("message"));
    }

    @Test
    public void testConventionNotMatched() {
        graphReport.conventionNotMatched(createSourceComponent("test:1"), "[\\w+\\s+]+\\s+[\\d\\.]+$", "Quarkus 1.4.*");

        JsonArray report = graphReport.dumpRecords(false);
        JsonObject record = report.getJsonObject(0);
        System.out.println(record);

        Assertions.assertEquals(1, report.size());
        Assertions.assertEquals("test:1", record.getString("component"));
        Assertions.assertEquals("Value does not match the convention. Value: 'Quarkus 1.4.*'. Pattern: [\\w+\\s+]+\\s+[\\d\\.]+$", record.getString("message"));
    }

    @Test
    public void testExceptionThrown() {
        graphReport.exceptionThrown(createSourceComponent("test:1"), new Exception("Test Exception"));

        JsonArray report = graphReport.dumpRecords(false);
        JsonObject record = report.getJsonObject(0);
        System.out.println(record);

        Assertions.assertEquals(1, report.size());
        Assertions.assertEquals("test:1", record.getString("component"));
        Assertions.assertTrue(record.getString("message")
                .matches("Exception was thrown while handling 'test:1': Test Exception\\nStacktrace:\\n\\[.*testExceptionThrown\\(TestReportHelper.java.*"));
    }

    @Test
    public void testEmpty() {
        Assertions.assertEquals(0, this.graphReport.dumpRecords(false).size());
    }

    @Test
    public void testAddOneRecord() {
        this.graphReport.addRecord(new JsonObject().put("id", "1"));

        JsonArray report = this.graphReport.dumpRecords(false);
        Assertions.assertEquals(1, report.size());
        Assertions.assertEquals("1", report.getJsonObject(0).getString("id"));
    }

    @Test
    public void testAddTwoRecords() {
        this.graphReport.addRecord(new JsonObject().put("id", "1"));
        this.graphReport.addRecord(new JsonObject().put("id", "2"));

        JsonArray report = this.graphReport.dumpRecords(false);
        Assertions.assertEquals(2, report.size());
        Assertions.assertEquals("1", report.getJsonObject(0).getString("id"));
        Assertions.assertEquals("2", report.getJsonObject(1).getString("id"));
    }

    private JsonObject createSourceComponent(String id) {
        return new JsonObject().put("id", id);
    }

}
