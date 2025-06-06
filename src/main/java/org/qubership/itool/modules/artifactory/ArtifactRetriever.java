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

package org.qubership.itool.modules.artifactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.apache.commons.lang3.StringUtils;
import org.qubership.itool.utils.FutureUtils;
import org.qubership.itool.utils.JsonUtils;
import org.qubership.itool.utils.XmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ArtifactRetriever {

    public static final String GRAPH_CLASSIFIER = "graph";
    public static final String GRAPH_GZ_CLASSIFIER = "graph";

    public static final String JSON_EXTENSION = "json";
    public static final String JSON_GZ_EXTENSION = "json.gz";

    public static final List<SnapshotFilter> GRAPH_FILTERS = List.of(
            new SnapshotFilter(GRAPH_CLASSIFIER, JSON_EXTENSION),
            new SnapshotFilter(GRAPH_GZ_CLASSIFIER, JSON_GZ_EXTENSION)
            );

    protected static final Logger LOG = LoggerFactory.getLogger(ArtifactRetriever.class);

    protected static final XPath xpath = XPathFactory.newInstance().newXPath();
    protected static final int HTTP_NOT_FOUND = 404;
    protected static final int HTTP_CONFLICT = 409;


    protected final WebClient webClient;
    // No need for credentials, proxies, etc for now


    public ArtifactRetriever(WebClient webClient) {
        this.webClient = webClient;
    }


    /**
     * Get list of versions for given component or application.
     * If maven-metadata.xml is not found, an empty list of versions will be returned.
     *
     * @param locator Describes component or application
     * @return A future that will return list of available versions
     */
    public Future<List<String>> retrieveArtifactVersions(ResourceLocator locator) {
        String urlString = locator.getBaseUrl() + "maven-metadata.xml";
        return downloadUrlAsBuffer(urlString)
            .transform(ar -> {
                if (ar.failed()) {
                    if (isNotFound(ar)) {
                        return Future.succeededFuture(Collections.emptyList());
                    }
                    return Future.failedFuture(ar.cause());
                }
                try {
                    Buffer buf = ar.result();
                    Document doc = XmlParser.parseXmlData(buf, urlString);
                    NodeList snapshotData = (NodeList) xpath.compile("/metadata/versioning/versions/version/text()")
                            .evaluate(doc, XPathConstants.NODESET);

                    int length = snapshotData.getLength();
                    List<String> collector = new ArrayList<>(length);
                    for (int i = 0; i < length; i++) {
                        Text item = (Text)snapshotData.item(i);
                        collector.add(item.getTextContent());
                    }
                    return Future.succeededFuture(collector);
                } catch (Exception e) {
                    return Future.failedFuture(new RetrievalException("Failure when parsing " + urlString, e));
                }
            });
    }

    /**
     * Get list of versions for given application using external applications database.
     *
     * @param appsDBintegration integration with external apps database
     * @param appName Application name
     * @return A future that will return list of versions on successful completion.
     * If application is not found in external apps DB, the future will fail.
     * If application is not found in artifactory, the future will contain an empty list of versions.
     */
    public Future<List<String>> retrieveAppVersions(AppsDBIntegration appsDBintegration, String appName) {
        return appsDBintegration.getAppResourceLocator(appName)
            .flatMap(locator -> retrieveArtifactVersions(locator));
    }

    /**
     * Download the latest snapshot of graph dump for given version of component or application.
     *
     * @param locator Describes component or application
     * @param versionId Artifact version (e.g.: "main-SNAPSHOT")
     * @return A future that will return GraphSnapshot (descriptor and data) on successful completion
     */
    public Future<GraphSnapshot> retrieveLatestGraphDump(ResourceLocator locator, String versionId) {
        return retrieveGraphDumpSnapshots(locator, versionId)
            .transform(descriptorsAr -> {
                if (descriptorsAr.failed()) {
                    if (isNotFound(descriptorsAr)) {
                        // maven-metadata.xml not found, trying to guess and download graphs
                        getLogger().warn("{} not found, falling back to manual search for artifacts",
                                ((RetrievalHttpException)descriptorsAr.cause()).urlString);
                        List<Future<GraphSnapshot>> downloads = downloadGraphArtifactsWithoutMavenMetadata(locator, versionId);
                        return FutureUtils.anyFuture(downloads);
                    } else {
                        return Future.failedFuture(descriptorsAr.cause());
                    }
                } else {
                    List<GraphSnapshot> descriptors = descriptorsAr.result();
                    getLogger().info("Graph snapshots for [{}:{}:{}] found: {}", locator.getGroupId(),
                            locator.getArtifactId(), versionId, descriptors);
                    if (descriptors.isEmpty()) {
                        return Future.failedFuture(new RetrievalException("No graph snapshots found for "
                                + locator.getGroupId() + ":" + locator.getArtifactId() + ":" + versionId));
                    }
                    GraphSnapshot descriptor = descriptors.get(0); // Get only the first graph
                    return retrieveGraphSnapshot(locator, versionId, descriptor);
                }
            });
    }

    /** Download a snapshot of version of graph dump for application using external applications database.
     *
     * @param appsDBIntegration apps DB integration
     * @param appName Application name
     * @param versionId Application version (e.g.: "main-SNAPSHOT")
     * @return A future that will return GraphSnapshot (descriptor and data) on successful completion
     *
     * @see #retrieveLatestGraphDump(ResourceLocator, String)
     */
    public Future<GraphSnapshot> retrieveLatestAppGraphDump(AppsDBIntegration appsDBIntegration, String appName, String versionId) {
        return appsDBIntegration.getAppResourceLocator(appName)
            .flatMap(locator -> retrieveLatestGraphDump(locator, versionId));
    }

    public Future<JsonSnapshot> retrieveLatestJsonSnapshot(ResourceLocator locator, String versionId, List<SnapshotFilter> filters) {
        return retrieveJsonSnapshots(locator, versionId, filters)
            .transform(descriptorsAr -> {
                if (descriptorsAr.failed()) {
                    if (isNotFound(descriptorsAr)) {
                        // maven-metadata.xml not found, trying to guess and download artifacts
                        getLogger().warn("{} not found, falling back to manual search for artifacts",
                                ((RetrievalHttpException)descriptorsAr.cause()).urlString);
                        List<Future<JsonSnapshot>> downloads = downloadJsonArtifactsWithoutMavenMetadata(locator, versionId, filters);
                        return FutureUtils.anyFuture(downloads);
                    } else {
                        return Future.failedFuture(descriptorsAr.cause());
                    }
                } else {
                    List<JsonSnapshot> descriptors = descriptorsAr.result();
                    getLogger().info("JSON snapshots for [{}:{}:{}] found: {}", locator.getGroupId(),
                            locator.getArtifactId(), versionId, descriptors);
                    if (descriptors.isEmpty()) {
                        return Future.failedFuture(new RetrievalException("No JSON snapshots found for ["
                                + locator.getGroupId() + ":" + locator.getArtifactId() + ":" + versionId
                                + "] and filters: " + filters));
                    }
                    JsonSnapshot descriptor = descriptors.get(0); // Get only the first JSON
                    return retrieveJsonSnapshot(locator, versionId, descriptor);
                }
            });
    }

    protected boolean isNotFound(AsyncResult<?> webResponse) {
        Throwable cause = webResponse.cause();
        if (cause instanceof RetrievalHttpException) {
            int statusCode = ((RetrievalHttpException)cause).getStatusCode();
            // In some crazy cases, artifactory responds with 409 instead of 404
            return statusCode == HTTP_NOT_FOUND || statusCode == HTTP_CONFLICT;
        } else {
            return false;
        }
    }

    /**
     * Download maven-metadata.xml for a version of component or application
     * and get snapshots for graphs.
     *
     * @param locator The resource locator
     * @param versionId Application/component version (e.g.: "main-SNAPSHOT")
     * @return A Future that will contain GraphSnapshot version descriptors, <b>without data</b>.
     */
    public Future<List<GraphSnapshot>> retrieveGraphDumpSnapshots(ResourceLocator locator, String versionId) {
        String urlString = locator.getBaseUrl(versionId) + "maven-metadata.xml";
        return downloadUrlAsBuffer(urlString)
            .flatMap(buf -> {
                try {
                    NodeList snapshotData = parseSnapshotVersions(urlString, buf);
                    List<GraphSnapshot> collector = new ArrayList<>();
                    for (int i = 0; i < snapshotData.getLength(); i++) {
                        Element item = (Element)snapshotData.item(i);
                        GraphSnapshot desc = toGraphSnapshot(item); // It can be a snapshot of something else than graph
                        if (GRAPH_FILTERS.stream().anyMatch(filter -> filter.matches(desc))) {
                            collector.add(desc);
                        }
                    }
                    return Future.succeededFuture(collector);
                } catch (Exception e) {
                    return Future.failedFuture(new RetrievalException("Failure when parsing " + urlString, e));
                }
            });
    }

    public Future<List<JsonSnapshot>> retrieveJsonSnapshots(ResourceLocator locator, String versionId, List<SnapshotFilter> filters) {
        String urlString = locator.getBaseUrl(versionId) + "maven-metadata.xml";
        return downloadUrlAsBuffer(urlString)
            .flatMap(buf -> {
                try {
                    NodeList snapshotData = parseSnapshotVersions(urlString, buf);
                    List<JsonSnapshot> collector = new ArrayList<>();
                    for (int i = 0; i < snapshotData.getLength(); i++) {
                        Element item = (Element)snapshotData.item(i);
                        JsonSnapshot desc = toJsonSnapshot(item);
                        if (filters.stream().anyMatch(filter -> filter.matches(desc))) {
                            collector.add(desc);
                        }
                    }
                    return Future.succeededFuture(collector);
                } catch (Exception e) {
                    return Future.failedFuture(new RetrievalException("Failure when parsing " + urlString, e));
                }
            });
    }

    protected NodeList parseSnapshotVersions(String urlString, Buffer buf) throws Exception {
        Document doc = XmlParser.parseXmlData(buf, urlString);
        NodeList snapshotData = (NodeList) xpath.compile("/metadata/versioning/snapshotVersions/snapshotVersion")
                .evaluate(doc, XPathConstants.NODESET);
        return snapshotData;
    }

    /**
     * Download maven-metadata.xml for a version of application
     * and get snapshots for graphs.
     *
     * @param appsDBIntegration apps DB integration
     * @param appName Application name
     * @param versionId Application version (e.g.: "main-SNAPSHOT")
     * @return A Future that will contain GraphSnapshot version descriptors, <b>without data</b>.
     */
    public Future<List<GraphSnapshot>> retrieveAppGraphDumpSnapshots(AppsDBIntegration appsDBIntegration, String appName, String versionId) {
        return appsDBIntegration.getAppResourceLocator(appName)
            .flatMap(locator -> retrieveGraphDumpSnapshots(locator, versionId));
    }

    protected List<Future<GraphSnapshot>> downloadGraphArtifactsWithoutMavenMetadata(ResourceLocator locator, String versionId) {
        return GRAPH_FILTERS.stream()
            .map(sf -> {
                String urlString = locator.getReleaseArtifactUrl(versionId, sf);
                return downloadJson(urlString, sf.getExtension())
                        .recover(f -> {
                            // Try to get artifact from snapshot folder if it is not a release version
                            if (versionId.endsWith("-RELEASE")) {
                                return Future.failedFuture(f);
                            }
                            if (f instanceof RetrievalHttpException) {
                                RetrievalHttpException ex = (RetrievalHttpException)f;
                                if (ex.getStatusCode() != HTTP_NOT_FOUND) {
                                    return Future.failedFuture(f);
                                }
                            }
                            return downloadJson(locator.getSnapshotArtifactUrl(versionId, sf), sf.getExtension());
                        })
                        .map(dump -> toGraphSnapshot(sf, urlString, dump) );
            })
            .collect(Collectors.toList());
    }

    protected List<Future<JsonSnapshot>> downloadJsonArtifactsWithoutMavenMetadata(
            ResourceLocator locator, String versionId, List<SnapshotFilter> filters)
    {
        return filters.stream()
            .map(sf -> {
                String urlString = locator.getReleaseArtifactUrl(versionId, sf);
                return downloadJson(urlString, sf.getExtension())
                        .map(json -> toJsonSnapshot(sf, urlString, json) );
            })
            .collect(Collectors.toList());
    }

    // Create snapshot with data for release without maven-metadata.xml
    protected GraphSnapshot toGraphSnapshot(SnapshotFilter sf, String urlString, JsonObject dump) {
        GraphSnapshot snapshot = new GraphSnapshot();
        snapshot.setOriginUrl(urlString);
        snapshot.setClassifier(sf.getClassifier());
        snapshot.setExtension(sf.getExtension());
        snapshot.setGraphDump(dump);
        return snapshot;
    }

    // Create snapshot with data for release without maven-metadata.xml
    protected JsonSnapshot toJsonSnapshot(SnapshotFilter sf, String urlString, JsonObject data) {
        JsonSnapshot snapshot = new JsonSnapshot();
        snapshot.setOriginUrl(urlString);
        snapshot.setClassifier(sf.getClassifier());
        snapshot.setExtension(sf.getExtension());
        snapshot.setData(data);
        return snapshot;
    }

    // Create snapshot without data from descriptor in maven-metadata.xml
    protected GraphSnapshot toGraphSnapshot(Element item) throws Exception {
        GraphSnapshot snapshot = new GraphSnapshot();
        Map<String, XPathExpression> fields = getSnapshotInfoFields();
        snapshot.setClassifier((String) fields.get("classifier").evaluate(item, XPathConstants.STRING));
        snapshot.setExtension((String) fields.get("extension").evaluate(item, XPathConstants.STRING));
        snapshot.setSnapshotId((String) fields.get("snapshotId").evaluate(item, XPathConstants.STRING));
        snapshot.setUpdated((String) fields.get("updated").evaluate(item, XPathConstants.STRING));
        return snapshot;
    }

    // Create snapshot without data from descriptor in maven-metadata.xml
    protected JsonSnapshot toJsonSnapshot(Element item) throws Exception {
        JsonSnapshot snapshot = new JsonSnapshot();
        Map<String, XPathExpression> fields = getSnapshotInfoFields();
        snapshot.setClassifier((String) fields.get("classifier").evaluate(item, XPathConstants.STRING));
        snapshot.setExtension((String) fields.get("extension").evaluate(item, XPathConstants.STRING));
        snapshot.setSnapshotId((String) fields.get("snapshotId").evaluate(item, XPathConstants.STRING));
        snapshot.setUpdated((String) fields.get("updated").evaluate(item, XPathConstants.STRING));
        return snapshot;
    }

    // Retrieve data and fill snapshot
    protected Future<GraphSnapshot> retrieveGraphSnapshot(ResourceLocator locator, String versionId, GraphSnapshot snapshot) {
        String urlString = locator.getArtifactUrl(versionId, snapshot);
        return downloadJson(urlString, snapshot.getExtension())
                .map(dump -> {
                    // Other fields of snapshot are already filled
                    snapshot.setOriginUrl(urlString);
                    snapshot.setGraphDump(dump);
                    return snapshot;
                });
    }

    // Retrieve data and fill snapshot
    protected Future<JsonSnapshot> retrieveJsonSnapshot(ResourceLocator locator, String versionId, JsonSnapshot snapshot) {
        String urlString = locator.getArtifactUrl(versionId, snapshot);
        return downloadJson(urlString, snapshot.getExtension())
                .map(json -> {
                    // Other fields of snapshot are already filled
                    snapshot.setOriginUrl(urlString);
                    snapshot.setData(json);
                    return snapshot;
                });
    }

    // Precompile some expressions on the first use. Do not care about true singletonity.
    private static volatile Map<String, XPathExpression> snapshotInfoFields;
    protected static Map<String, XPathExpression> getSnapshotInfoFields() throws Exception {
        if (snapshotInfoFields == null) {
            snapshotInfoFields = Map.of(
                "classifier", xpath.compile("classifier/text()"),
                "extension", xpath.compile("extension/text()"),
                "snapshotId", xpath.compile("value/text()"),
                "updated", xpath.compile("updated/text()")
            );
        }
        return snapshotInfoFields;
    }

    protected Future<JsonObject> downloadJson(String urlString, String extension) {
        return downloadUrlAsBuffer(urlString)
            .map(buffer -> {
                try {
                    switch (StringUtils.defaultString(extension)) {
                    case JSON_EXTENSION:
                        return JsonUtils.bufferToJsonObject(buffer);
                    case JSON_GZ_EXTENSION:
                        return JsonUtils.gzipBufferToJsonObject(buffer);
                    default:
                        throw new RetrievalException("Internal error: unrecognized extension: " + extension);
                    }
                } catch (IOException e) {
                    throw new RetrievalException(e);
                }
            });
    }

    protected Future<Buffer> downloadUrlAsBuffer(String urlString) {
        getLogger().info("Downloading: {}", urlString);
        return webClient.requestAbs(HttpMethod.GET, urlString)
            .send()
            .map(response -> {
                int statusCode = response.statusCode();
                if (statusCode == 200) {
                    getLogger().info("Found: {}", urlString);
                    return response.bodyAsBuffer();
                }
                // In some crazy cases, artifactory responds with 409 instead of 404
                if (statusCode == HTTP_NOT_FOUND || statusCode == HTTP_CONFLICT) {
                    getLogger().info("Not found: {}", urlString);
                }
                throw new RetrievalHttpException(urlString, response);
            });
    }

    public Future<GraphSnapshot> retrieveAppGraphDump(AppDBIntegrationImpl appDBIntegration, String appName, String versionId) {
        return appDBIntegration.getAppResourceLocator(appName)
                .flatMap(locator -> retrieveGraphDump(locator, versionId));
    }

    /**
     * Download the graph dump for given version of application.
     *
     * @param locator Describes application
     * @param versionId Artifact version (e.g.: "main-SNAPSHOT")
     * @return A future that will return GraphSnapshot (descriptor and data) on successful completion
     */
    public Future<GraphSnapshot> retrieveGraphDump(ResourceLocator locator, String versionId) {
        if (versionId == null){
            return Future.failedFuture("VersionId is null");
        }
        if (versionId.endsWith("-SNAPSHOT")) {
            getLogger().debug("Scheduling the retrieval of the latest graph snapshot for artifactId={} with version={}", locator.getArtifactId(), versionId);
            return retrieveLatestGraphDump(locator, versionId);
        } else {
            getLogger().debug("Scheduling the retrieval of the graph snapshot for artifactId={} with version={}", locator.getArtifactId(), versionId);
            List<Future<GraphSnapshot>> downloads = downloadGraphArtifactsWithoutMavenMetadata(locator, versionId);
            return FutureUtils.anyFuture(downloads);
        }
    }

    protected Logger getLogger() {
        return LOG;
    }
}
