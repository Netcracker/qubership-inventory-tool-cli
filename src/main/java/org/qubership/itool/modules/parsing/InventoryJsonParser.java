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

package org.qubership.itool.modules.parsing;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.qubership.itool.modules.report.GraphReport;
import org.qubership.itool.utils.JsonUtils;
import org.qubership.itool.utils.LanguageUtils;

import javax.annotation.Resource;
import static org.qubership.itool.modules.graph.Graph.F_DNS_NAME;
import static org.qubership.itool.modules.graph.Graph.F_DNS_NAMES;
import static org.qubership.itool.utils.JsonUtils.*;

public class InventoryJsonParser {

    @Resource
    protected GraphReport report;

    public void parse(JsonObject domain, JsonObject component, String inventorySource) {
        parse(domain, component, new JsonObject(inventorySource));
    }

    public void parse(JsonObject domain, JsonObject component, JsonObject inventoryJson) {
        fillDetails(domain, component, inventoryJson);
        fillFeatures(component, inventoryJson);
    }

    private void fillDetails(JsonObject domain, JsonObject component, JsonObject inventoryJson) {
        JsonObject detailsJson = getOrCreateJsonObject(component, "details");

        String abbreviation = inventoryJson.getString("id");
        detailsJson.put("abbreviation", abbreviation);
        component.put("abbreviation", abbreviation);

        String name = inventoryJson.getString("name");
        detailsJson.put("name", name);
        component.put("name", name);

        String owner = inventoryJson.getString("owner");
        putValueIfNotNull(detailsJson, "owner", owner);

        String dnsName = inventoryJson.getString("dnsName");
        JsonArray altDnsNames = inventoryJson.getJsonArray("altDnsNames");
        fillDnsNames(detailsJson, dnsName, altDnsNames);

        String domainFromInventory = inventoryJson.getString("domain");
        putValueIfNotNull(detailsJson, "domainFromInventory", domainFromInventory);

        detailsJson.put("domain", domain.getValue("id"));

        String description = inventoryJson.getString("description");
        putValueIfNotNull(detailsJson, "description", description);

        String type = inventoryJson.getString("type");
        putValueIfNotNull(detailsJson, "type", type);
        putValueIfNotNull(component, "type", type);

        Object framework = inventoryJson.getValue("framework");
        putValueIfNotNull(detailsJson, "framework", convertListToFilteredString(framework));

        JsonArray documentation = inventoryJson.getJsonArray("documentation");
        putValueIfNotNull(detailsJson, "documentationLink", documentation);

        Object tmfSpec = inventoryJson.getValue("tmfSpec");
        if (tmfSpec != null) {
            if (tmfSpec instanceof JsonObject) {
                detailsJson.put("tmfSpec", convertToLegacyTmfSpec((JsonObject) tmfSpec));
            } else {
                report.addMessage(GraphReport.CONF_ERROR, component,
                    component + ": TMF Spec has wrong type mentioned- "
                        + tmfSpec.getClass());
            }
        }

        Object language = inventoryJson.getValue("language");
        putValueIfNotNull(detailsJson, "language", LanguageUtils.convertListToLanguages(language));

        fillApiDocumentation(detailsJson, inventoryJson);
        processDatabases(detailsJson, inventoryJson);
        processQueues(detailsJson, inventoryJson);

        JsonObject dependency = inventoryJson.getJsonObject("dependency");
        putValueIfNotNull(detailsJson, "dependencies", dependency);
        copyValueIfNotNull(inventoryJson, detailsJson, "thirdparty");

    }

    private static void fillDnsNames(JsonObject detailsJson, String dnsName, JsonArray altDnsNames) {
        JsonArray dnsNames = new JsonArray();
        if (dnsName != null) {
            detailsJson.put(F_DNS_NAME, dnsName);
            dnsNames.add(dnsName);
        }
        if (altDnsNames != null) {
            dnsNames.addAll(altDnsNames);
        }
        detailsJson.put(F_DNS_NAMES, dnsNames);
    }

    private void fillApiDocumentation(JsonObject details, JsonObject inventoryJson) {
        JsonObject apiJson = new JsonObject();
        details.put("api", apiJson);

        Boolean openApi = inventoryJson.getBoolean("openApi", false);
        if (openApi != null) {
            apiJson.put("openApi", openApi ? "yes" : "no");
        }

        Object apisp = inventoryJson.getValue("openAPIpublished");
        if (apisp instanceof JsonArray) {
            JsonArray apiSpecPublished = (JsonArray) apisp;
            apiJson.put("apiSpecPublished", apiSpecPublished);
        } else if (apisp instanceof String) {
            JsonArray apiSpecPublished = new JsonArray().add(apisp);
            apiJson.put("apiSpecPublished", apiSpecPublished);
        }
    }

    private void processDatabases(JsonObject details, JsonObject inventoryJson) {
        JsonArray source = inventoryJson.getJsonArray("database");
        if (source == null) {
            return;
        }

        JsonArray target = JsonUtils.getOrCreateJsonArray(details, JsonPointer.from("/database/database"));
        for (Object o : source) {
            JsonObject jo = JsonUtils.asJsonObject(o);
            String name = jo.getString("name");
            // Put database name like "PostgreSQL 12" to "/details/database/database[]/item"
            // SetEdgesBetweenComponentsVerticle.processDatabases() will link this component to vertex "PostgreSQL"
            String version = jo.getString("version");
            if (StringUtils.isNotEmpty(version)) {
                name = name + " " + version;
            }
            JsonObject dbItem = new JsonObject()
                .put("item", name)
                .put("viaZookeeper", "no");
            target.add(dbItem);
        }
    }

    private void processQueues(JsonObject details, JsonObject inventoryJson) {
        JsonArray source = inventoryJson.getJsonArray("queue");
        if (source == null) {
            return;
        }

        JsonObject target = JsonUtils.getOrCreateJsonObject(details, "messageQueues");
        for (Object o : source) {
            JsonObject jo = (JsonObject) o;
            String type = jo.getString("type", "").toLowerCase();
            if ("rabbitmq".equals(type)) {
                type = "rabbitMQ";
            }
            String role = jo.getString("role", "consumer");
            String qName = jo.getString("name", "");
            JsonObject qtype = JsonUtils.getOrCreateJsonObject(target, type);
            JsonUtils.getOrCreateJsonArray(qtype, role).add(qName);
        }
    }

    private void fillFeatures(JsonObject component, JsonObject inventoryJson) {
        JsonObject featuresJson = new JsonObject();
        component.put("features", featuresJson);

        parseFeaturesMultitenancy(featuresJson, inventoryJson);
    }

    private void parseFeaturesMultitenancy(JsonObject featuresJson, JsonObject inventoryJson) {
        JsonObject multitenancyJson = new JsonObject();
        featuresJson.put("multitenancy", multitenancyJson);

        String defaultTenantId = (Boolean) JsonPointer.from("/multitenancy/defaultTenantId")
            .queryJsonOrDefault(inventoryJson, Boolean.FALSE) ? "yes" : "no";
        multitenancyJson.put("defaultTenantId", defaultTenantId);
    }

    /*
     * Converts "tmfSpec": {"622": { "version": ["18.0.3", "19.0.1"] }, "666": { "version": ["18.0.3"] }}
     * to "tmfSpec": ["622 v18.0.3", "622 v19.0.1", "666 v18.0.3"]
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static JsonArray convertToLegacyTmfSpec(JsonObject spec) {
        JsonArray result = new JsonArray();
        for (Map.Entry<String, Object> item : spec.getMap().entrySet()) {
            String tmfSpecNumber = item.getKey();
            for (String tmfSpecVersion : getVersions(new JsonObject((Map) item.getValue()))) {
                result.add(tmfSpecNumber + " v" + tmfSpecVersion);
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getVersions(JsonObject version) {
        if (version == null) {
            return Collections.emptyList();
        }
        Object tmfSpecVersions = version.getValue("version");
        if (tmfSpecVersions == null) {
            return Collections.emptyList();
        } else if (tmfSpecVersions instanceof JsonArray) {
            return ((JsonArray) tmfSpecVersions).getList();
        } else if (tmfSpecVersions instanceof String) {
            return Collections.singletonList((String) tmfSpecVersions);
        } else {
            return Collections.singletonList(version.getString("version"));
        }
    }

}
