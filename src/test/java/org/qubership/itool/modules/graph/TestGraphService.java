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

package org.qubership.itool.modules.graph;

import com.google.common.cache.CacheStats;
import io.vertx.core.Vertx;
import org.qubership.itool.modules.report.GraphReport;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestGraphService {

    private GraphServiceImpl graphService;
    private Vertx vertx;
    private GraphFactory graphFactory;
    private GraphReportFactory graphReportFactory;

    @BeforeAll
    public void setupAll() {
        vertx = Vertx.vertx();
        graphFactory = new DefaultGraphFactory(new DefaultGraphReportFactory());
        graphReportFactory = new DefaultGraphReportFactory();
    }

    @AfterAll
    public void cleanupAll() {
        vertx.close();
    }

    @BeforeEach
    public void setup() {
        GraphManager graphManager = new GraphManager(vertx, null, false, graphFactory, graphReportFactory) {

            @Override
            public CacheStats getCacheStatistics() {
                return null;
            }

            @Override
            public Graph buildGraphByClassifier(GraphClassifier classifier) {
                Graph graph = graphFactory.createGraph();
                if (classifier.isWithReport()) {
                    GraphReport report = graphReportFactory.createGraphReport();
                    graph.setReport(report);
                }
                return graph;
            }
        };
        this.graphService = new GraphServiceImpl(graphManager);
    }

    @Test
    public void testPutAndGet() {
        String id1 = "id1";
        GraphClassifier classifier11 = new GraphClassifierBuilderImpl()
                .addApplicationVersionId(id1)
                .setWithReport(false)
                .build();

        Graph graph11 = graphService.getGraphByClassifier(classifier11);
        // Now Graph is present without report
        assertNotNull(graph11);

        assertSame(graph11, graphService.getGraphByClassifier(classifier11));

        GraphClassifier classifier12 = new GraphClassifierBuilderImpl()
                .addApplicationVersionId(id1)
                .setWithReport(true)
                .build();

        System.out.println(">>>>>> id1 generated: " + classifier11.getId());
        assertEquals(classifier11.getId(), classifier12.getId());   //MD5-generated

        Graph graph12 = graphService.getGraphByClassifier(classifier12);
        // Now another Graph is present with report
        assertNotNull(graph12);
//        assertNotSame(graph11, graph12);

        // Now classifier11 refers to the same graph as classifier12
        assertSame(graph12, graphService.getGraphByClassifier(classifier11));

        // Try another id
        String id2 = "id2";
        GraphClassifier classifier21 = new GraphClassifierBuilderImpl()
                .addApplicationVersionId(id2)
                .build();

        System.out.println(">>>>>> id2 generated: " + classifier21.getId());
        assertNotEquals(classifier11.getId(), classifier21.getId());    //MD5-generated

        Graph graph21 = graphService.getGraphByClassifier(classifier21);
        assertNotNull(graph21);
        assertNotSame(graph21, graph11);
        assertNotSame(graph21, graph12);
    }
}
