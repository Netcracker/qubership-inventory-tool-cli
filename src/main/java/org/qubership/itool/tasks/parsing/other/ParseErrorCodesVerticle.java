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

package org.qubership.itool.tasks.parsing.other;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Emphasis;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.tasks.parsing.AbstractParseFileTask;
import org.qubership.itool.utils.FSUtils;
import org.qubership.itool.utils.GitUtils;
import org.qubership.itool.utils.ProcessingIssueBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.qubership.itool.modules.graph.Graph.F_ID;
import static org.qubership.itool.modules.graph.Graph.F_NAME;
import static org.qubership.itool.modules.graph.Graph.F_TYPE;
import static org.qubership.itool.modules.graph.Graph.V_ERROR_CODE;
import static org.qubership.itool.modules.parsing.MdParserUtils.LOWERCASE_EXTRACTOR;
import static org.qubership.itool.modules.parsing.MdParserUtils.collectCells;
import static org.qubership.itool.modules.parsing.MdParserUtils.collectParagraphs;
import static org.qubership.itool.modules.parsing.MdParserUtils.collectRows;
import static org.qubership.itool.modules.parsing.MdParserUtils.collectSiblings;
import static org.qubership.itool.modules.parsing.MdParserUtils.findColumnIdxByText;
import static org.qubership.itool.modules.parsing.MdParserUtils.findHeading;
import static org.qubership.itool.modules.parsing.MdParserUtils.findTableBody;
import static org.qubership.itool.modules.parsing.MdParserUtils.findTableHead;
import static org.qubership.itool.utils.ProcessingIssueCategories.PARSING;

public class ParseErrorCodesVerticle extends AbstractParseFileTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParseErrorCodesVerticle.class);

    private final Parser parser;

    public ParseErrorCodesVerticle() {
        List<Extension> extensions = Arrays.asList(TablesExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
    }

    @Override
    protected String[] getFilePatterns() {
        return new String[]{
                //            "docs/troubleshooting-guide.md",
                "docs/**/troubleshooting-guide.md",
                "docs/troubleshooting/errors/*.md",
                "documents/troubleshooting/errors/*.md",
                "documents/**/troubleshooting-guide.md",
        };
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected void parseSingleFile(JsonObject domain, JsonObject component, String fileName) throws IOException {
        if (fileName.endsWith("troubleshooting-guide.md")) {
            parseTroubleshootingGuide(domain, component, fileName);
        } else {
            parseSingleError(domain, component, fileName);
        }
    }

    private void parseTroubleshootingGuide(JsonObject domain, JsonObject component, String fileName)
            throws IOException {
        String data = FSUtils.readFileSafe(fileName);
        Node doc = parser.parse(data);

        Node pointer = doc.getFirstChild();
        while (pointer != null) {
            TableHead head = findTableHead(pointer);
            // no more tables
            if (head == null) {
                break;
            }
            TableBody body = findTableBody(head);
            List<TableCell> headCells = collectCells(head.getFirstChild().getFirstChild());
            Integer errorCodeIdx = findColumnIdxByText(headCells, "Error Code");
            Integer messageIdx = findColumnIdxByText(headCells, "Error Message",
                    "Message Text (English)", "Message Text");
            Integer scenarioIdx = findColumnIdxByText(headCells, "Scenario");
            Integer reasonIdx = findColumnIdxByText(headCells, "Reason");
            Integer solutionIdx = findColumnIdxByText(headCells, "Solution");

            if (errorCodeIdx != null && messageIdx != null) {
                List<TableRow> rows = collectRows(body.getFirstChild());
                for (TableRow row : rows) {
                    List<TableCell> cells = collectCells(row.getFirstChild());

                    // Handle crazy cells in "Error Code" column like "ABC-0001<br/>ABCDEF-0001"
                    List<String> errorCodes = collectSiblings(cells.get(errorCodeIdx).getFirstChild())
                            .stream()
                            .filter(node -> node instanceof Text)
                            .map(text -> ((Text) text).getLiteral())
                            .collect(Collectors.toList());

                    String errorMessage = getContentsofCell(cells, messageIdx);

                    String scenario = null;
                    if (scenarioIdx != null) {
                        scenario = getContentsofCell(cells, scenarioIdx);
                    }

                    String reason = null;
                    if (reasonIdx != null) {
                        reason = getContentsofCell(cells, reasonIdx);
                    }

                    String solution = null;
                    if (solutionIdx != null) {
                        solution = getContentsofCell(cells, solutionIdx);
                    }
                    for (String code : errorCodes) {
                        addErrorCode(component, fileName, code, errorMessage, scenario, reason, solution);
                    }
                }
            }

            // Try another table
            pointer = body.getParent();
        }
    }

    private String getContentsofCell(List<TableCell> cells, Integer cellIdx) {
        String content = null;
        Node cellContents = cells.get(cellIdx).getFirstChild();
        if (cellContents instanceof Text) {
            // Error message cell may contain some crazy HTML besides just Text. Ignore it...
            content = ((Text) cellContents).getLiteral();
        } else if (cellContents instanceof Emphasis) {
            // A crazy cell with underscores (Emphasis)
            content = ((Text) cellContents.getFirstChild()).getLiteral();
        }

        return content;
    }

    private void parseSingleError(JsonObject domain, JsonObject component, String fileName) throws IOException {
        String code = Path.of(fileName).getFileName().toString().replaceFirst("\\.md$", "");

        String data = FSUtils.readFileSafe(fileName);
        Node doc = parser.parse(data);
        String messageText = null;
        String scenarioText = null;
        String reasonText = null;
        String solutionText = null;

        Node nameHeading = findHeading(doc.getFirstChild(), 2, code);
        Node textHeading = findHeading(nameHeading, 3, "message text", LOWERCASE_EXTRACTOR);
        Node textHeadingScenario = findHeading(nameHeading, 3, "scenario", LOWERCASE_EXTRACTOR);
        Node textHeadingReason = findHeading(nameHeading, 3, "reason", LOWERCASE_EXTRACTOR);
        Node textHeadingSolution = findHeading(nameHeading, 3, "solution", LOWERCASE_EXTRACTOR);
        if (textHeading != null) {
            messageText = getTextOfParagraph(textHeading);
        }
        if (textHeadingScenario != null) {
            scenarioText = getTextOfParagraph(textHeadingScenario);
        }

        if (textHeadingReason != null) {
            reasonText = getTextOfParagraph(textHeadingReason);
        }

        if (textHeadingSolution != null) {
            solutionText = getTextOfParagraph(textHeadingSolution);
        }

        addErrorCode(component, fileName, code, messageText, scenarioText, reasonText, solutionText);
    }

    private String getTextOfParagraph(Node textHeading) {
        List<Paragraph> paras = collectParagraphs(textHeading.getNext());
        return paras.isEmpty() ? null
                : paras.stream()
                        .map(para -> ((Text) para.getFirstChild()).getLiteral())
                        .collect(Collectors.joining("\n"));
    }

    /**
     * Generates a unique vertex ID for an error code by combining the error code
     * with a hash of the component identifier to ensure uniqueness per component.
     *
     * @param code error code identifier
     * @param componentId component identifier
     * @return unique vertex ID in format: code-hash
     */
    private String generateErrorCodeVertexId(String code, String componentId) {
        int hash = Objects.hash(code, componentId);
        return code + "-" + Integer.toHexString(hash);
    }

    private void addErrorCode(JsonObject component, String fileName, String code,
            String messageText, String scenarioText, String reasonText, String solutionText) {
        String componentId = component.getString(F_ID);
        String errorCodeVertexId = generateErrorCodeVertexId(code, componentId);
        JsonObject details = new JsonObject()
                .put("describedIn", GitUtils.buildRepositoryLink(component, fileName, config()));
        JsonArray processingErrors = new JsonArray();
        if (messageText != null) {
            details.put("messageText", messageText);
        } else {
            report.mandatoryValueMissed(component, "/errorCode/" + code + "/messageText");
            JsonObject processingError = new ProcessingIssueBuilder(PARSING, "Message text is missing")
                .source("ParseErrorCodesVerticle", "")
                .timestamp(Instant.now())
                .details(d -> {
                    d.put("file", fileName);
                    d.put("code", code);
                })
                .build();
            processingErrors.add(processingError);
        }
        if (scenarioText != null) {
            details.put("scenario", scenarioText);
        }
        if (reasonText != null) {
            details.put("reason", reasonText);
        }
        if (solutionText != null) {
            details.put("solution", solutionText);
        }
        Graph graph = this.graph;
        JsonObject errorCode = new JsonObject()
                .put(F_ID, errorCodeVertexId)
                .put(F_TYPE, V_ERROR_CODE)
                .put(F_NAME, code)
                .put("details", details);
        if (!processingErrors.isEmpty()) {
            ProcessingIssueBuilder.ensureIssuesArray(errorCode).addAll(processingErrors);
        }
        JsonObject edge = new JsonObject()
                .put(F_TYPE, V_ERROR_CODE);

        graph.addEdge(component, errorCode, edge);
    }

}
