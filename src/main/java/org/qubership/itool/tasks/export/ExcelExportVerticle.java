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

package org.qubership.itool.tasks.export;

import org.qubership.itool.tasks.FlowTask;

import io.vertx.core.Promise;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellUtil;
import org.qubership.itool.utils.FSUtils;
import org.qubership.itool.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;
import static org.qubership.itool.modules.graph.Graph.F_DNS_NAME;
import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_NAME;
import static org.qubership.itool.modules.graph.Graph.F_REPOSITORY;
import static org.qubership.itool.modules.graph.Graph.F_TYPE;
import static org.qubership.itool.modules.graph.Graph.V_LIBRARY;
import static org.qubership.itool.modules.graph.Graph.V_MICROSERVICE;
import static org.qubership.itool.utils.ConfigProperties.EXCEL_EXPORT_PROPERTY;

public class ExcelExportVerticle extends FlowTask {
    protected Logger LOG = LoggerFactory.getLogger(ExcelExportVerticle.class);

    Map<String, CellStyle> cellStylesMap = new HashMap<>();
    private final static String COMP_NAME_STYLE = "compNameStyle";
    private final static String ERROR_COMP_NAME_STYLE = "errorCompNameStyle";
    private final static String PROPERTY_STYLE = "propertyStyle";

    public static Map<String, String> readablePropertyToFieldNameMap = Map.ofEntries(
        Map.entry("Owner", "owner"),
        Map.entry("Abbreviation", "abbreviation"),
        Map.entry("DNS name", F_DNS_NAME),
        Map.entry("Domain", "domain"),
        Map.entry("Description", "description"),
        Map.entry("TMF spec", "tmfSpec"),
        Map.entry("Type", "type"),
        Map.entry("Sticky sessions", "stickySessions"),
        Map.entry("Language", "language"),
        Map.entry("Framework", "framework"),
        Map.entry("Reactive", "reactive")
    );

    @Override
    protected String[] features() {
        return new String[] { "excelExport" };
    }

    @Override
    protected void taskStart(Promise<?> taskPromise) throws Exception {
        String exportPath = config().getString(EXCEL_EXPORT_PROPERTY);
        if (exportPath == null) {
            LOG.error("excelExport property is not set");
        }
        LOG.info("Scheduling blocking execution of ExcelExport process in a separate thread");
        WorkerExecutor executor = vertx.createSharedWorkerExecutor("excel-export-worker-pool"
                , 1
                , 60
                , TimeUnit.MINUTES);
        String finalExportPath = exportPath;
        executor.executeBlocking(() -> {
            try {
                buildInventorizationXls(finalExportPath);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to build Excel export " + finalExportPath + ": " + e.getMessage());
            }
            return null;
        }).onSuccess(r -> taskCompleted(taskPromise)).onFailure(r -> {
            report.exceptionThrown(new JsonObject().put("id", "iTool"), (Exception) r);
            taskCompleted(taskPromise);
        });

    }

    private Map<String, CellStyle> buildCustomCellStyles(HSSFWorkbook book) {
        Map<String, CellStyle> cellStylesMap = new HashMap<>();

        Font normalCompNameFont = book.createFont();
        normalCompNameFont.setBold(true);

        Font errorCompNameFont = book.createFont();
        errorCompNameFont.setColor(IndexedColors.RED.getIndex());
        errorCompNameFont.setBold(true);

        CellStyle compNameStyle = book.createCellStyle();
        compNameStyle.setWrapText(true);
        compNameStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        compNameStyle.setFillForegroundColor(HSSFColorPredefined.LIGHT_BLUE.getIndex());
        compNameStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        compNameStyle.setFont(normalCompNameFont);
        cellStylesMap.put(COMP_NAME_STYLE, compNameStyle);

        CellStyle errorCompNameStyle = book.createCellStyle();
        errorCompNameStyle.setWrapText(true);
        errorCompNameStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        errorCompNameStyle.setFillForegroundColor(HSSFColorPredefined.LIGHT_BLUE.getIndex());
        errorCompNameStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        errorCompNameStyle.setFont(errorCompNameFont);
        cellStylesMap.put(ERROR_COMP_NAME_STYLE, errorCompNameStyle);

        CellStyle propertyStyle = book.createCellStyle();
        propertyStyle.setWrapText(true);
        propertyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        propertyStyle.setFillForegroundColor(HSSFColorPredefined.LIGHT_BLUE.getIndex());
        propertyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        propertyStyle.setFont(normalCompNameFont);
        cellStylesMap.put(PROPERTY_STYLE, propertyStyle);
        return cellStylesMap;
    }

    private void addSheetContent(Sheet sheet, JsonObject domain) {
        List<JsonObject> components = graph.getSuccessors(domain.getString(F_ID), false)
                .stream()
                .filter(o -> o.getString("repository") != null && o.getJsonObject("details") != null)
                .collect(Collectors.toList());
        int row = 0;
        addComponentNamesRow(sheet, components, row++);
        addDetailsRow(sheet, components, "Owner", row++);
        addDetailsRow(sheet, components, "Abbreviation", row++);
        addDetailsRow(sheet, components, "DNS name", row++);
        addDetailsRow(sheet, components, "Domain", row++);
        addDetailsRow(sheet, components, "Description", row++);
        addDetailsRow(sheet, components, "TMF spec", row++);
        addDetailsRow(sheet, components, "Type", row++);
        addDetailsRow(sheet, components, "Sticky sessions", row++);
        addDetailsRow(sheet, components, "Language", row++);
        addDetailsRow(sheet, components, "Framework", row++);
        addDetailsRow(sheet, components, "Reactive", row++);
        addPropertyRow(sheet, components, "Database", row++,
                c -> JsonUtils.getOrCreateJsonArray(c, JsonPointer.from("/details/database/database")).stream()
                    .map(e -> ((JsonObject) e).getString("item"))
                    .collect(Collectors.joining(", ")));
        addPropertyRow(sheet, components, "External index", row++,
                c -> JsonPointer.from("/details/database/externalIndices/item").queryJson(c).toString());
        addPropertyRow(sheet, components, "External cache", row++,
                c -> JsonPointer.from("/details/database/externalCache/item").queryJson(c).toString());

        addSectionNameRow(sheet, components, "RabbitMQ:", row++);
        addPropertyRow(sheet, components, "  * Producer (Exchanges)", row++,
                c -> retrievePropertyListAsString("/details/messageQueues/rabbitMQ/producer", c));
        addPropertyRow(sheet, components, "  * Consumer (Queues)", row++,
                c -> retrievePropertyListAsString("/details/messageQueues/rabbitMQ/consumer", c));

        addSectionNameRow(sheet, components, "Kafka:", row++);
        addPropertyRow(sheet, components, "  * Producer (Topics)", row++,
                c -> retrievePropertyListAsString("/details/messageQueues/kafka/producer", c));
        addPropertyRow(sheet, components, "  * Consumer (Topics)", row++,
                c -> retrievePropertyListAsString("/details/messageQueues/kafka/consumer", c));

        addPropertyRow(sheet, components, "Startup dependency", row++,
                c -> retrievePropertyListAsString("/details/dependencies/startup", c));
        addPropertyRow(sheet, components, "Mandatory dependency", row++,
                c -> retrievePropertyListAsString("/details/dependencies/mandatory", c));
        addPropertyRow(sheet, components, "Optional dependency", row++,
                c -> retrievePropertyListAsString("/details/dependencies/optional", c));
        addPropertyRow(sheet, components, "Git repository", row++, c -> c.getString(F_REPOSITORY));
        addPropertyRow(sheet, components, "Confluence article", row++,
                c -> retrievePropertyListAsString("/details/documentationLink", c));
        addPropertyRow(sheet, components, "OpenAPI/Swagger", row++,
                c -> JsonPointer.from("/details/api/openApi").queryJson(c).toString());
        addPropertyRow(sheet, components, "API spec published", row++,
                c -> ((JsonArray) JsonPointer.from("/details/api/apiSpecPublished").queryJson(c))
                    .stream().map(String.class::cast).collect(Collectors.joining(", ")));
        addPropertyRow(sheet, components, "API versioning", row++,
                c -> JsonPointer.from("/details/api/apiVersioning").queryJson(c).toString());

        addPropertyRow(sheet, components, "ZK DB connection support", row++,
                c -> JsonUtils.getOrCreateJsonArray(c, JsonPointer.from("/details/database/database")).stream()
                    .map(e -> ((JsonObject) e).getString("viaZookeeper"))
                    .collect(Collectors.joining(", ")));

        addSectionNameRow(sheet, components, "Blue-Green:", row++);
        addPropertyRow(sheet, components, "  * HTTP request", row++,
                c -> JsonPointer.from("/features/blueGreen/httpRequest").queryJson(c).toString());
        addPropertyRow(sheet, components, "  * HTTP callback", row++,
                c -> JsonPointer.from("/features/blueGreen/httpCallback").queryJson(c).toString());
        addPropertyRow(sheet, components, "  * Zeebe workers", row++,
                c -> JsonPointer.from("/features/blueGreen/zeebeWorkers").queryJson(c).toString());
        addPropertyRow(sheet, components, "  * MessageQueue", row++,
                c -> JsonPointer.from("/features/blueGreen/messageQueueConsumers").queryJson(c).toString());
    }

    protected String retrieveLibsString(String pointer, JsonObject c) {
        JsonArray dependencies = (JsonArray) JsonPointer.from(pointer).queryJson(c);
        if (dependencies.size() == 0) {
            return "N/A";
        } else {
            return String.join(", ",
                    dependencies.stream().map(d -> ((JsonObject) d).getString("artifactId"))
                            .collect(Collectors.toList()));
        }
    }

    protected String retrievePropertyListAsString(String pointer, JsonObject c) {
        JsonArray results = (JsonArray) JsonPointer.from(pointer).queryJson(c);
        if (results == null || results.size() == 0) {
            return "";
        }
        return String.join(", ", results.getList());
    }

    public void buildInventorizationXls(String exportFileLocation) throws IOException {
        String reportFolderName = FSUtils.getFolder(exportFileLocation);
        File reportFolder = new File(reportFolderName);
        if (!reportFolder.exists()) {
            reportFolder.mkdirs();
        }
        try (HSSFWorkbook book = new HSSFWorkbook()) {

            HSSFPalette palette = book.getCustomPalette();
            palette.setColorAtIndex(HSSFColorPredefined.LIGHT_BLUE.getIndex(), (byte) 189, (byte) 215, (byte) 238);

            cellStylesMap = buildCustomCellStyles(book);

            List<JsonObject> domains = V().hasType("domain").toList();

            for (JsonObject domain : domains) {
                Sheet sheet = book.createSheet(domain.getString(F_ID));
                addSheetContent(sheet, domain);
                reFormatSheet(sheet, domain);
            }

            try (OutputStream os = new FileOutputStream(
                    reportFolder + File.separator + getFileName(exportFileLocation))) {
                book.write(os);
            }
        }
    }

    private String getFileName(String exportFileLocation) {
        String fileName = FilenameUtils.getBaseName(exportFileLocation);
        String extension = FilenameUtils.getExtension(exportFileLocation);
        return fileName + LocalDateTime.now().format(DateTimeFormatter.ofPattern("_yyyy-MM-dd_HHmmss.")) + extension;
    }

    private void reFormatSheet(Sheet sheet, JsonObject domain) {
        sheet.autoSizeColumn(0);
        List<JsonObject> components = graph.getSuccessors(domain.getString(F_ID), false)
                .stream()
                .filter(o -> V_MICROSERVICE.equals(o.getString(F_TYPE)) || V_LIBRARY.equals(o.getString((F_TYPE))))
                .collect(Collectors.toList());
        for (int i = 1; i <= components.size(); i++) {
            sheet.setColumnWidth(i, 6000);
        }
    }


    protected void addComponentNamesRow(Sheet sheet, List<JsonObject> components, int rowIndex) {
        addRow(sheet, components, rowIndex, cell -> {
                },
                (cell, component) -> {
                    cell.setCellStyle(cellStylesMap.get(COMP_NAME_STYLE));
                    CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
                    String name = component.getJsonObject("details").getString(F_NAME);
                    cell.setCellValue((name == null) ? "unknown" : name.replace(" ", "\n"));
                });
    }

    private void addRow(Sheet sheet, List<JsonObject> components, int rowIndex,
                        Consumer<Cell> headerCellBuilder, BiConsumer<Cell, JsonObject> cellBuilder) {
        Row row = sheet.createRow(rowIndex);

        int columnIndex = 0;
        Cell nameCell = row.createCell(columnIndex++);
        headerCellBuilder.accept(nameCell);

        for (JsonObject component : components) {
            Cell cell = row.createCell(columnIndex++);
            cellBuilder.accept(cell, component);
        }
    }

    protected void addSectionNameRow(Sheet sheet, List<JsonObject> components, String propertyName, int rowIndex) {
        addRow(sheet, components, rowIndex, cell -> {
            cell.setCellStyle(cellStylesMap.get(PROPERTY_STYLE));
            cell.setCellValue(propertyName);
        }, (cell, component) -> {
            cell.setCellStyle(cellStylesMap.get(PROPERTY_STYLE));
        });
    }

    protected void addDetailsRow(Sheet sheet, List<JsonObject> components, String propertyName, int rowIndex) {
        addPropertyRow(sheet, components, propertyName, rowIndex,
                component -> {
                    Object obj = component.getJsonObject("details")
                            .getValue(readablePropertyToFieldNameMap.get(propertyName));
                    if (obj instanceof String) {
                        return (String)obj;
                    } else if (obj instanceof JsonArray) {
                        @SuppressWarnings("unchecked")
                        String result = (String) ((JsonArray)obj).getList()
                                .stream()
                                .map(o -> o.toString())
                                .collect(Collectors.joining(", "));
                        return result;
                    }
                    return "";
                });
    }

    protected void addPropertyRow(Sheet sheet, List<JsonObject> components, String propertyName,
            int rowIndex, Function<JsonObject, String> cellValueProducer) {
        addRow(sheet, components, rowIndex, cell -> {
                    cell.setCellStyle(cellStylesMap.get(PROPERTY_STYLE));
                    cell.setCellValue(propertyName);
                }, (cell, component) -> {
                    String propertyValue = "";
                    try {
                        propertyValue = cellValueProducer.apply(component);
                    } catch (NullPointerException e) {
                        propertyValue = "Field was not found in model";
                        cell.setCellStyle(cellStylesMap.get(ERROR_COMP_NAME_STYLE));
                    }
                    cell.setCellValue(propertyValue);
                }
        );
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
