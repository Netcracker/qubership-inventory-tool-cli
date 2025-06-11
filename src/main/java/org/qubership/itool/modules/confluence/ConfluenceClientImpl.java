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

package org.qubership.itool.modules.confluence;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientSession;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class ConfluenceClientImpl implements ConfluenceClient {

    private static final Logger LOG = LoggerFactory.getLogger(ConfluenceClientImpl.class);

    // TODO: @Resource is not working yet, ApplicationContext update required.
    //  Client and Vertx are passed in constructor so far;
    private WebClient client;
    private Vertx vertx;
    private JsonObject config;

    private WebClientSession session;
    private Credentials credentials;

    protected ConfluenceClientImpl(Vertx vertx, WebClient client, JsonObject config) {
        this.vertx = vertx;
        this.client = client;
        this.config = config;
        this.credentials = (new UsernamePasswordCredentials(config.getString("login"), config.getString("password")))
                .applyHttpChallenge(null);
        this.session = WebClientSession.create(client);
    }

    @Override
    public Future<JsonObject> updateConfluencePage(String spaceKey, String title, String parentTitle, String filePath, String release) {
        LOG.info("Sending page '{}' stored in '{}' to Confluence", title, filePath);
        Future<JsonObject> futureConfluencePageInfo = getConfluencePageInfo(spaceKey, title, release)
                // If page was not found, then get the parent id, create new page and return its info in future
                .recover(pageInfo -> {
                    LOG.debug("'{}': Page was not found, searching for the parent '{}' and create the new one", title, parentTitle);
                    return getConfluencePageInfo(spaceKey, parentTitle, release)
                            .compose(parent -> createConfluencePage(spaceKey, title, parent.getString("id"), release));
                });
        // Read the page file and convert it to storage format
        Future<String> futureStorageFileContents = readFile(filePath)
                .compose(wikiFileContent -> convertWikiToStorage(wikiFileContent, title)
                        .compose(storageContent -> saveFile(buildStorageFileName(filePath), storageContent)
                                .recover(e -> {
                                    // Saving contents is not critical, so we'll rather not fail the whole process here
                                    LOG.warn("Failed to save contents in storage format to file {}: {}",
                                            buildStorageFileName(filePath), ExceptionUtils.getStackTrace(e));
                                    return Future.succeededFuture(storageContent);
                                })
                        )
                );

        Future<JsonObject> futureResponse = CompositeFuture.join(futureConfluencePageInfo, futureStorageFileContents)
                .compose(compositeFuture -> {
                            JsonObject confluencePageInfo = futureConfluencePageInfo.result();
                            String storageFileContents = futureStorageFileContents.result();
                            JsonArray ancestors = confluencePageInfo.getJsonArray("ancestors");
                            Future<JsonObject> parentFuture;
                            JsonObject parentPage = ancestors.getJsonObject(ancestors.size()-1);
                            if (buildPageTitle(parentTitle, release).equals(parentPage.getString("title"))){
                                parentFuture = Future.succeededFuture(parentPage);
                            } else {
                                // If parent title doesn't match the closest ancestor, it shall be changed
                                LOG.debug("'{}': Parent page didn't match, finding the related parent with title '{}'", title, parentTitle);
                                parentFuture = getConfluencePageInfo(spaceKey, parentTitle, release);
                            }
                            return parentFuture.compose(parent ->
                                    uploadConfluencePageAsStorage(confluencePageInfo, parent.getString("id"), storageFileContents));
                        },
                        fail -> Future.failedFuture("Failed to upload the content of '" + title + "' page, reason: " + fail.getMessage()));
        return futureResponse;
    }

    private String buildStorageFileName(String path) {
        Path fullPath = Path.of(path);
        String fileName = fullPath.getFileName().toString().replaceFirst("\\.\\w+", "") + "_storage.xml";
        return fullPath.subpath(0, fullPath.getNameCount()-1).resolve(fileName).toString();
    }

    @Override
    public Future<JsonObject> updateConfluencePage(JsonObject page, String release) {
        String spaceKey = page.getString("space");
        String title = page.getString("title");
        String parentTitle = page.getString("parentTitle");
        String onDiskPath = page.getString("onDiskPath");
        return updateConfluencePage(spaceKey, title, parentTitle, onDiskPath, release);
    }

    @Override
    public Future<JsonObject> createOrMoveConfluencePage(String spaceKey, String title, String parentId, String release) {
        Future<JsonObject> future = getConfluencePageInfo(spaceKey, title, release)
                .compose(page -> moveConfluencePage(page, parentId, release),
                        res -> createConfluencePage(spaceKey, title, parentId, release));
        return future;
    }

    @Override
    public Future<JsonObject> createConfluencePage(String spaceKey, String title, String parentId, String release) {
        String finalTitle = buildPageTitle(title, release);
        JsonObject request = buildPageCreateRequestBody(finalTitle, spaceKey, parentId);
        LOG.info("Attempting to create the page '{}'", finalTitle);
        return session.post(443, config.getString("confluenceUrl"), "/rest/api/content")
                .followRedirects(true)
                .ssl(true)
                .authentication(credentials)
                .sendJsonObject(request)
                .compose(rsp -> {
                    LOG.debug("Creation of page '{}' finished", finalTitle);
                    if (200 != rsp.statusCode()) {
                        return Future.failedFuture("Can't create confluence page: " + rsp.bodyAsString());
                    }
                    return Future.succeededFuture(rsp.bodyAsJsonObject());
                });
    }

    @Override
    public Future<JsonArray> getChildPages(String spaceKey, String pageId) {
        LOG.debug("Getting child pages for page with id={}", pageId);
        Future<JsonArray> future = getChunkedList(spaceKey, "/rest/api/content/" + pageId + "/child/page");
//                    LOG.trace("Found {} child pages retrieval for id={} finished", children.size(), pageId);
        return future;
    }

    private Future<JsonArray> getChunkedList(String spaceKey, String url) {
        Future<JsonArray> arrayFuture = Future.future(promise -> {
            getNextChunk(url)
                    .onSuccess(body -> {
                        JsonArray children = body.getJsonArray("results");
                        // No more pages, returning this array
                        if (!body.getJsonObject("_links").containsKey("next")) {
                            promise.complete(children);
                            return;
                        }
                        String next = body.getJsonObject("_links").getString("next");
                        LOG.trace("More pages present, loading next: {}", next);
                        getChunkedList(spaceKey, next)
                                .onSuccess(nextChildren -> {
                                    children.addAll(nextChildren);
                                    promise.complete(children);
                                });
                    });
            });
        return arrayFuture;
    }

    private Future<JsonObject> getNextChunk(String next) {
        LOG.trace("Getting next batch of pages from {}", next);
        Future<JsonObject> future = session.get(443, config.getString("confluenceUrl"), next)
                .followRedirects(true)
                .ssl(true)
                .authentication(credentials)
                .addQueryParam("expand", "version")
                .send()

                .compose(response -> {
                    if (200 != response.statusCode()) {
                        return Future.failedFuture("Can't receive pages from " + next
                                + " from Confluence: " + response.statusMessage());
                    }
                    return Future.succeededFuture(response.bodyAsJsonObject());
                });
        return future;
    }

    private JsonObject buildPageCreateRequestBody(String title, String space, String parentId) {
        JsonObject requestBody = new JsonObject()
                .put("type", "page")
                .put("title", title)
                .put("space", new JsonObject().put("key", space))
                .put("ancestors", new JsonArray().add(new JsonObject().put("id", parentId)));
        return requestBody;
    }

    @Override
    public Future<JsonObject> moveConfluencePage(JsonObject page, String newParentId, String release) {
        String finalTitle = page.getString("title");
        LOG.info("Attempting to move the page '{}' to new parent with id={}", finalTitle, newParentId);
        JsonObject request = buildPageMoveRequestBody(finalTitle, newParentId,
                page.getJsonObject("version").getInteger("number"));
        Future<JsonObject> future = session.put(443, config.getString("confluenceUrl"), "/rest/api/content/" + page.getString("id"))
                .followRedirects(true)
                .ssl(true)
                .authentication(credentials)
                .sendJsonObject(request)

                .compose(rsp -> {
                    LOG.debug("Movement of page '{}' finished", finalTitle);
                    if (200 != rsp.statusCode()) {
                        return Future.failedFuture("Can't move Confluence page '" + page.getString("title") + "': " + rsp.bodyAsString());
                    }
                    return Future.succeededFuture(rsp.bodyAsJsonObject());
                });
        return future;
    }

    private JsonObject buildPageMoveRequestBody(String title, String parentId, Integer version) {
        JsonObject requestBody = new JsonObject()
                .put("type", "page")
                .put("title", title)
                .put("ancestors", new JsonArray().add(new JsonObject().put("id", parentId)))
                .put("version", new JsonObject().put("number", version + 1));
        return requestBody;
    }

    @Override
    public Future<String> convertWikiToStorage(String wikiContent, String title) {
        LOG.info("Converting the wiki format for '{}' into storage format", title);
        JsonObject request = new JsonObject()
                .put("value", wikiContent)
                .put("representation", "wiki");

        Future<String> future = session.post(443, config.getString("confluenceUrl"), "/rest/api/contentbody/convert/storage")
                .followRedirects(true)
                .ssl(true)
                .authentication(credentials)
                .sendJsonObject(request)

                .compose( rsp -> {
                    LOG.trace("Conversion of page '{}' finished", title);
                    if (200 != rsp.statusCode()) {
                        return Future.failedFuture("Can't convert wiki page '" + title + "' to storage format, response: "
                                + rsp.bodyAsString());
                    }
                    return Future.succeededFuture(rsp.bodyAsJsonObject().getString("value"));
                });
        return future;
    }

    @Override
    public Future<JsonObject> getConfluencePageInfo(String spaceKey, String title) {
        return getConfluencePageInfo(spaceKey, title, null);
    }

    @Override
    public Future<JsonObject> getConfluencePageInfo(String spaceKey, String title, String release) {
        String finalTitle = buildPageTitle(title, release);
        LOG.debug("Retrieving detailed page information from Confluence for page '{}'", finalTitle);
        Future<JsonObject> future = client.get(443, config.getString("confluenceUrl"), "/rest/api/content")
                .addQueryParam("spaceKey", spaceKey)
                .addQueryParam("title", finalTitle)
                .addQueryParam("expand", "version,ancestors")
                .ssl(true)
                .authentication(credentials)
                .send()

                .compose(response -> {
                    LOG.trace("Detailed page retrieval for '{}' finished", finalTitle);

                    if (200 != response.statusCode()) {
                        return Future.failedFuture("Can't receive page info for '" + finalTitle
                                + "' from Confluence: " + response.statusCode() + ":" + response.statusMessage());
                    }
                    JsonObject confluencePageInfoJson = response.bodyAsJsonObject();
                    if (confluencePageInfoJson.getJsonArray("results").size() == 1) {
                        return Future.succeededFuture(confluencePageInfoJson.getJsonArray("results").getJsonObject(0));
                    }
                    if (confluencePageInfoJson.getJsonArray("results").size() > 1) {
                        return Future.failedFuture("Found more than one page with a spaceKey=" + spaceKey + " and title='" + finalTitle + "'");
                    }
                    return Future.failedFuture("Page '" + finalTitle + "' was not found in Confluence");
                });
        return future;
    }

    @Override
    public Future<JsonObject> getConfluencePageInfo(String pageId) {
        LOG.info("Retrieving detailed page information from Confluence for page with id={}", pageId);
        Future<JsonObject> future = client.get(443, config.getString("confluenceUrl"), "/rest/api/content/" + pageId)
                .addQueryParam("expand", "version,ancestors")
                .ssl(true)
                .authentication(credentials)
                .send()

                .compose(response -> {
                    if (200 != response.statusCode()) {
                        return Future.failedFuture("Failed to retrieve info for page id=" + pageId
                                + " from Confluence: " + response.bodyAsString());
                    }
                    return Future.succeededFuture(response.bodyAsJsonObject());
                });
        return future;
    }

    private String buildPageTitle(String title, String release) {
        return release == null ? title : release + " " + title;
    }

    @Override
    public Future<JsonObject> uploadConfluencePageAsStorage(JsonObject page, String parentId, String storageContent) {
        LOG.debug("Uploading page '{}' with id {} and parentId {} in storage format.", page.getString("title"),
                page.getString("id"), parentId);
        JsonObject requestBody = buildUpdateRequestBody(page, parentId, storageContent);
        Future<JsonObject> future = client.put(443, config.getString("confluenceUrl"), "/rest/api/content/" + page.getString("id"))
                .authentication(credentials)
                .ssl(true)
                .followRedirects(true)
                .sendJsonObject(requestBody)

                .compose( rsp -> {
                    if (200 != rsp.statusCode()) {
                        return Future.failedFuture(("Page update failed for page '" + page.getString("title") + "': "
                                + rsp.bodyAsString()));
                    }
                    LOG.debug("Uploading of page '{}' with storage format finished", page.getString("title"));
                    return Future.succeededFuture(rsp.bodyAsJsonObject());
                });
        return future;
    }

    private JsonObject buildUpdateRequestBody(JsonObject page, String parentId, String storageContent) {
        JsonObject requestBody = new JsonObject()
                .put("type", "page")
                .put("title", page.getString("title"))
                .put("version", new JsonObject()
                        .put("number", page.getJsonObject("version").getInteger("number") + 1)
                )
                .put("body", new JsonObject()
                        .put("storage", new JsonObject()
                                .put("value", storageContent)
                                .put("representation", "storage")
                        )
                );
        if (parentId != null) {
            requestBody.put("ancestors", new JsonArray().add(new JsonObject().put("id", parentId)));
        }
        return requestBody;
    }

    private Future<String> readFile(String path) {
        LOG.trace("Reading file contents of {}", path);
        Future<String> future = vertx.fileSystem().readFile(path)
                .compose(res -> {
                    LOG.trace("Reading of file '{}' finished", path);
                    return Future.succeededFuture(res.toString());
                }, fail -> Future.failedFuture("Reading of file " + path + " failed: " + fail.getCause().getMessage()));
        return future;
    }

    private Future<String> saveFile(String path, String content) {
        LOG.trace("Saving file contents of {}", path);
        Future<String> future = vertx.fileSystem().writeFile(path, Buffer.buffer(content))
                .compose(res -> {
                    LOG.trace("Saving of file '{}' finished", path);
                    return Future.succeededFuture(content);
                }, fail -> Future.failedFuture("Saving of file " + path + " failed: " + fail.getCause().getMessage()));
        return future;
    }

}
