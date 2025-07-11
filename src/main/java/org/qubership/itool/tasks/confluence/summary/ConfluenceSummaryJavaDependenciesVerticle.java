package org.qubership.itool.tasks.confluence.summary;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.tuple.Pair;
import org.qubership.itool.modules.gremlin2.P;
import org.qubership.itool.modules.gremlin2.graph.GraphTraversal;
import org.qubership.itool.modules.template.ConfluencePage;
import org.qubership.itool.tasks.confluence.AbstractConfluenceGenerationPageVerticle;
import org.qubership.itool.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import jakarta.inject.Provider;

import static org.qubership.itool.modules.gremlin2.P.neq;
import static org.qubership.itool.modules.gremlin2.graph.__.outE;

public class ConfluenceSummaryJavaDependenciesVerticle extends AbstractConfluenceGenerationPageVerticle {
    protected Logger LOG = LoggerFactory.getLogger(ConfluenceSummaryJavaDependenciesVerticle.class);

    @Resource
    Provider<ConfluencePage> confluencePageProvider;

    @Override
    protected List<ConfluencePage> prepareConfluencePageList(String department) {
        return null; // do nothing
    }

    @Override
    protected List<ConfluencePage> prepareConfluencePageList() {
        // key=component, value=List<libraries>
        List<Pair<JsonObject, List<JsonObject>>> componentDirectDep     = new ArrayList<>();
        List<Pair<JsonObject, List<JsonObject>>> componentTransitiveDep = new ArrayList<>();

        List<JsonObject> components = V().hasType("domain").out().toList();
        for (JsonObject component : components) {
            // directDependencies ================================
            String componentId = component.getString("id");
            GraphTraversal<JsonObject, JsonObject> directTraversal =
                V(componentId).as("C").out("module")
                .outE("dependency")
                    .has("scope", neq("test"))
                    .has("component", componentId)
                .inV().dedup();
            componentDirectDep.add(Pair.of(component, directTraversal.clone().toList()));

            // transitiveDependencies ============================
            List<JsonObject> transitiveDependencies =
                 directTraversal.clone()
                .outE("dependency")
                    .has("scope", neq("test"))
                    .has("component", componentId)
                .inV().dedup()
                .repeat(
                     outE("dependency")
                        .has("scope", neq("test"))
                        .has("component", componentId)
                    .inV().dedup()
                ).emit().dedup()
                .toList();
            componentTransitiveDep.add(Pair.of(component, transitiveDependencies));
        }

        // key: componentId, value: dependency data
        Map<String, JsonObject> map = new HashMap<>();

        addDependenciesToMap(map, componentDirectDep, "directDependencies");
        addDependenciesToMap(map, componentTransitiveDep, "transitiveDependencies");

        List<JsonObject> librariesList = map.values().stream().map(
                depData -> new JsonObject()
                        .put("groupId", depData.getString("groupId"))
                        .put("artifactId", depData.getString("artifactId"))
                        .put("version", depData.getString("version"))
                        .put("directDependencies", depData.getJsonArray("directDependencies"))
                        .put("transitiveDependencies", depData.getJsonArray("transitiveDependencies")))
                .sorted((o1, o2) -> {
                            String o1groupId = o1.getString("groupId");
                            String o1artifactId = o1.getString("artifactId");
                            String o1version = o1.getString("version");
                            String o2groupId = o2.getString("groupId");
                            String o2artifactId = o2.getString("artifactId");
                            String o2version = o2.getString("version");

                            if (o1groupId.equals(o2groupId) &&
                                    o1artifactId.equals(o2artifactId) &&
                                    o1version.equals(o2version)) {
                                return 0;
                            }

                            if (!o1groupId.equals(o2groupId)) {
                                return o1groupId.compareTo(o2groupId);
                            }

                            if (!o1artifactId.equals(o2artifactId)) {
                                return o1artifactId.compareTo(o2artifactId);
                            }

                            if (o1version != null && o2version == null) {
                                return 1;
                            }

                            if (o1version == null && o2version != null) {
                                return -1;
                            }

                            return P.lteVersion(o1version).test(o2version) ? -1 : 1;
                        })
                .collect(Collectors.toList());

        List<ConfluencePage> confluencePageList = new ArrayList<>();

        ConfluencePage page = confluencePageProvider.get();
        confluencePageList.add(page);

        page.setTitle("Java Dependencies");
        page.setParentTitle("Summary");
        page.setType("summary");
        page.setTemplate("summary/javaDependencies.ftlh");
        page.setDirectoryPath("summary/");
        page.setFileName("javaDependencies");

        page.addDataModel("librariesList", librariesList);
        return confluencePageList;
    }

    private void addDependenciesToMap(Map<String, JsonObject> map,
            List<Pair<JsonObject, List<JsonObject>>> componentDep, String depType) {
        for (Pair<JsonObject, List<JsonObject>> data : componentDep) {
            List<JsonObject> directDep = data.getRight();
            for (JsonObject dep : directDep) {
                JsonObject javaDependency = map.computeIfAbsent(dep.getString("id"),
                    key -> new JsonObject()
                            .put("groupId", dep.getString("groupId"))
                            .put("artifactId", dep.getString("artifactId"))
                            .put("version", dep.getString("version")));
                JsonArray directDeps = JsonUtils.getOrCreateJsonArray(javaDependency, depType);
                directDeps.add(data.getLeft());
            }
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
