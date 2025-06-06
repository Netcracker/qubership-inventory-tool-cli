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

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ConfluenceClient {
    Future<JsonObject> updateConfluencePage(String spaceKey, String title, String parentTitle, String filePath, String release);

    Future<JsonObject> updateConfluencePage(JsonObject page, String release);

    Future<JsonObject> createOrMoveConfluencePage(String spaceKey, String title, String parentId, String release);

    Future<JsonObject> getConfluencePageInfo(String spaceKey, String title);

    Future<JsonObject> getConfluencePageInfo(String spaceKey, String title, String release);

    Future<JsonObject> createConfluencePage(String spaceKey, String title, String parentId, String release);

    Future<JsonArray> getChildPages(String spaceKey, String pageId);

    Future<JsonObject> moveConfluencePage(JsonObject page, String parentId, String release);

    Future<String> convertWikiToStorage(String wikiContent, String title);

    Future<JsonObject> getConfluencePageInfo(String pageId);

    Future<JsonObject> uploadConfluencePageAsStorage(JsonObject page, String parentId, String storageContent);
}
