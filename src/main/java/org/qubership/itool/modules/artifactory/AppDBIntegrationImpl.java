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

import com.google.common.net.UrlEscapers;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Integration with external database that maps applications to locations in artifactory server(s).
 * external database registries are cached lazily, application entries are loaded on every request.
 */
public class AppDBIntegrationImpl implements AppsDBIntegration {

    protected static final Logger LOG = LoggerFactory.getLogger(AppDBIntegrationImpl.class);

    protected final WebClient webClient;
    protected final AppDBConfig config;

    protected final ConcurrentMap<String, ArtifactoryConfig> registryCache = new ConcurrentHashMap<>();
    // TODO? application location cache

    public AppDBIntegrationImpl(WebClient webClient, AppDBConfig config) {
        this.webClient = webClient;
        this.config = config;
    }


    //------------------------------------------------------
    // Public API

    /**
     * Get list of application names from Applications database.
     *
     * @return A future that will return list of application names on successful completion
     */
    @Override
    public Future<List<String>> getApplicationNames() {
        String url = config.getUrl() + "applications/view";
        getLogger().info("Downloading: {} using Basic auth: {}:******", url, config.getUser());

        return webClient.requestAbs(HttpMethod.GET, url)
            .basicAuthentication(config.getUser(), config.getPassword())
            .send()
            .map(response ->
                 responseToJsonArray(url, response)
                .stream()
                .map(obj -> getString(obj, "name"))
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList())
            );
    }

    /**
     * Get location of given application from applications database
     * @param appName A name of application in applications database
     * @return A Future for a Locator resolved from applications database.
     */
    @Override
    public Future<ResourceLocator> getAppResourceLocator(String appName) {
        // java.net.URLEncoder is not appropriate here
        String url = config.getUrl() + "applications/" + UrlEscapers.urlPathSegmentEscaper().escape(appName);
        getLogger().info("Downloading: {} using Basic auth: {}:******", url, config.getUser());

        return webClient.requestAbs(HttpMethod.GET, url)
            .basicAuthentication(config.getUser(), config.getPassword())
            .send()
            .map(response -> responseToJsonObject(url, response))
            .flatMap(appConfig ->
                 getArtifactory(appConfig)
                .map(artifactoryConfig -> {
                    String groupId = appConfig.getString("appGid");
                    String artifactId = appConfig.getString("appAid");
                    getLogger().debug("Mapped: {} -> {}:{} in {}", appName, groupId, artifactId, artifactoryConfig);
                    return new ResourceLocator(artifactoryConfig, ArtifactKind.APPLICATION, groupId, artifactId);
                })
            )
            .recover(t -> {
                if (t instanceof RetrievalException) {
                    return Future.failedFuture(t);
                } else {
                    return Future.failedFuture(new RetrievalException("Failed to get application data from applications database", t));
                }
            });
    }

    @Override
    public void clearCaches() {
        registryCache.clear();
    }

    //------------------------------------------------------

    @Override
    public Future<ArtifactoryConfig> getArtifactory(JsonObject appConfig) {
        String registryName = appConfig.getString("registryName");
        String appName = appConfig.getString("name");
        getLogger().debug("Application '{}' resides in registry '{}'", appName, registryName);

        ArtifactoryConfig artConf = registryCache.get(registryName);
        if (artConf != null) {
            return Future.succeededFuture(artConf);
        }

        // java.net.URLEncoder is not appropriate here
        String url = config.getUrl() + "registry/" + UrlEscapers.urlPathSegmentEscaper().escape(registryName);
        getLogger().info("Downloading: {} using Basic auth: {}:******", url, config.getUser());
        return webClient.requestAbs(HttpMethod.GET, url)
                .basicAuthentication(config.getUser(), config.getPassword())
                .send()
                .map(response -> responseToJsonObject(url, response))
                .map(regConfig -> {
                    String server = (String) JsonPointer.from("/maven/REPOSITORY_DOMAIN_NAME").queryJson(regConfig);
                    String storage = null;

                    String mavenEntryList = config.getMavenEntry();
                    String[] mavenEntries = mavenEntryList.split(",");
                    for (String entry : mavenEntries) {
                        storage = (String) JsonPointer.from("/maven/" + entry.trim()).queryJson(regConfig);
                        if (StringUtils.isNotEmpty(storage)) {
                            break;
                        }
                    }

                    if (StringUtils.isEmpty(storage)){
                        throw new RetrievalException("Registry " + registryName + " has no configured storages "
                                + config.getMavenEntry() + " for application" + appName);
                    }

                    ArtifactoryConfig ac = new ArtifactoryConfig(server, storage);
                    getLogger().debug("Registry '{}', entries '{}' -> {}", registryName, config.getMavenEntry(), ac);
                    registryCache.put(registryName, ac);
                    return ac;
                });
    }

    @Override
    public JsonObject responseToJsonObject(String url, HttpResponse<Buffer> response) {
        checkResponse(url, response);
        return response.bodyAsJsonObject();
    }

    @Override
    public JsonArray responseToJsonArray(String url, HttpResponse<Buffer> response) {
        checkResponse(url, response);
        return response.bodyAsJsonArray();
    }

    @Override
    public void checkResponse(String url, HttpResponse<Buffer> response) {
        int statusCode = response.statusCode();
        if (statusCode != 200) {
            throw new RetrievalHttpException(url, response);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public String getString(Object container, String key) {
        if (container instanceof JsonObject) {
            return ((JsonObject) container).getString(key);
        } else if (container instanceof Map) {
            return (String) ((Map) container).get(key);
        } else {
            return null;
        }
    }

    public Logger getLogger() {
        return LOG;
    }
}
