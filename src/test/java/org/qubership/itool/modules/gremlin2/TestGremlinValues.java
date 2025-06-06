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

package org.qubership.itool.modules.gremlin2;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.*;

import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.modules.gremlin2.graph.GraphTraversal;
import org.qubership.itool.modules.gremlin2.structure.MapElement;
import org.qubership.itool.modules.gremlin2.util.Order;
import org.qubership.itool.utils.JsonUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.qubership.itool.modules.gremlin2.P.*;
import static org.qubership.itool.modules.gremlin2.graph.__.count;
import static org.qubership.itool.modules.gremlin2.graph.__.has;
import static org.qubership.itool.modules.gremlin2.graph.__.is;

public class TestGremlinValues extends AbstractGremlinTest {

    @Test
    public void testOrder() {
        List<Integer> result = V().<Integer>value("age").order().toList();
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(29, result.get(0));
        Assertions.assertEquals(32, result.get(1));
    }

    @Test
    public void testOrderByKey() {
        List<Map<String, Long>> result = V().group().by("type").by(count())
                .<Map<String, Long>>unfold().by(MapElement.both)
                .order().by(MapElement.key).by(Order.asc)
                .toList();
        assertEquals(4, result.size());

        assertEquals(Map.of("os", 1L), result.get(0));
        assertEquals(Map.of("person", 2L), result.get(1));

        assertEquals(1, result.get(2).size());
        assertEquals("root", result.get(2).keySet().iterator().next());

        assertEquals(Map.of("soft", 2L), result.get(3));
    }

    @Test
    public void testOrderByValue() {
        List<Map<String, Long>> result = V().group().by("type").by(count())
                .<Map<String, Long>>unfold().by(MapElement.both)
                .order().by(MapElement.value).by(Order.asc)
                .toList();
        assertEquals(4, result.size());

        // "root" and "os", each mapped to 1
        assertEquals(1L, result.get(0).values().iterator().next());
        assertEquals(1L, result.get(1).values().iterator().next());
        Set<String> types01 = new HashSet<>();
        types01.add(result.get(0).keySet().iterator().next());
        types01.add(result.get(1).keySet().iterator().next());
        assertEquals(Set.of("root", "os"), types01);

        // "person" and "soft", each mapped to 2
        assertEquals(2L, result.get(2).values().iterator().next());
        assertEquals(2L, result.get(3).values().iterator().next());
        Set<String> types23 = new HashSet<>();
        types23.add(result.get(2).keySet().iterator().next());
        types23.add(result.get(3).keySet().iterator().next());
        assertEquals(Set.of("person", "soft"), types23);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testDeepValues() throws Exception {
        // Add one more vertex
        JsonObject newVertex = JsonUtils.readJsonResource(getClass(), "classpath:/valuesDeepArray.json");
        String newVertexId = newVertex.getString(Graph.F_ID);
        graph.addVertexUnderRoot(newVertex);

        // Path in JsonObject
        assertSame(newVertex, V(newVertexId).next());
        assertEquals("abc", V(newVertexId).value("/deep/a/b/c").next());
        assertEquals("abc", V(newVertexId).value("deep").value("/a/b").value("c").next());
        assertEquals("abc", V(newVertexId).value("deep").value("a").value("/b").value("c").next());

        assertSame(newVertex, V(newVertexId).as("C").select("C").next());
        assertEquals("abc", V(newVertexId).as("C").select("C").value("/deep/a/b/c").next());

        // Path in Map/JsonObject
        Map<String, Object> selectCAsMap = V(newVertexId).as("C").select(new String[] {"C"}).next();
        assertTrue(selectCAsMap instanceof Map);
        assertSame(newVertex, selectCAsMap.get("C"));

        assertEquals("abc", V(newVertexId).as("C").select(new String[] {"C"})
            .value("/C/deep/a/b/c").next());
        assertEquals("abc", V(newVertexId).as("C").select(new String[] {"C"})
            .value("/C/deep").value("/a/b").value("c").next());
        assertEquals("abc", V(newVertexId).as("C").select(new String[] {"C"})
            .value("C").value("deep").value("a").value("/b").value("c").next());

        // Path in Map/Map/...
        List<?> unfolded = V(newVertexId).value("structured").unfold().toList();
        assertEquals(1, unfolded.size());
        assertTrue(unfolded.get(0) instanceof Map);
        assertTrue( ((Map)unfolded.get(0)).get("spec") instanceof Map);

        assertEquals(List.of(100), V(newVertexId).value("structured").unfold()
            .value("spec").value("/template").value("spec").value("affinity").value("podAntiAffinity")
            .value("/preferredDuringSchedulingIgnoredDuringExecution[]").unfold().value("weight").toList());
        assertEquals(List.of(100), V(newVertexId).value("structured").unfold()
            .value("spec").value("/template").value("spec").value("affinity").value("podAntiAffinity")
            .value("preferredDuringSchedulingIgnoredDuringExecution").unfold().value("weight").toList());
        assertEquals(List.of(100), V(newVertexId).value("structured").unfold()
            .value("/spec/template").value("spec").value("affinity").value("podAntiAffinity")
            .value("/preferredDuringSchedulingIgnoredDuringExecution[]").unfold().value("weight").toList());
        assertEquals(List.of(100), V(newVertexId).value("structured").unfold()
            .value("/spec/template/spec/affinity/podAntiAffinity")
            .value("/preferredDuringSchedulingIgnoredDuringExecution[]/weight").unfold().toList());
        assertEquals(List.of(100), V(newVertexId).value("/structured[]/spec/template/spec"
                + "/affinity/podAntiAffinity/preferredDuringSchedulingIgnoredDuringExecution[]/weight")
            .unfold().unfold().toList());   // 2 * '[]', 2 * unfold() ==> List of scalars
        assertEquals(List.of(List.of(100)), V(newVertexId).value("/structured[]/spec/template/spec"
                + "/affinity/podAntiAffinity/preferredDuringSchedulingIgnoredDuringExecution[]/weight")
            .unfold().toList());    // 2 * '[]', 1 * unfold() ==> List of Lists

        // Scalars in List
        assertEquals(List.of("-jar", "/app/quarkus.jar"),
            V(newVertexId).value("structured").unfold()
           .value("/spec/template/spec/containers").unfold()
           .value("args").unfold().is(containing("jar")).toList());

        // Test value pointer "/" with scalars
        assertEquals(List.of("-jar", "/app/quarkus.jar"),
             V(newVertexId).value("structured").unfold()
            .value("/spec/template/spec/containers").unfold()
            .value("args").unfold().has("/", containing("jar")).toList());
    }

    @Test
    public void testDedup() {
       List<String> result = V().<String>value("lang").dedup().toList();
       Assertions.assertEquals(List.of("java"), result);
    }

    @Test
    public void testDedupBy() {
        List<Map<Object, String>> result =
            V().<String>values("id", "lang").dedup().by("lang").toList();
        Assertions.assertEquals(1, result.size());
    }

    @Test
    public void testDedupByJsonPointer() {
        List<Map<Object, String>> result =
            V().<String>values("id", "lang").dedup().by("/lang").toList();
        Assertions.assertEquals(1, result.size());
    }

    @Test
    public void testGenericValueString() {
        List<String> result = V().<String>value("lang").toList();
        Assertions.assertEquals(List.of("java", "java"), result);
    }

    @Test
    public void testGenericValueInteger() {
        List<Integer> result = V().<Integer>value("age").toList();
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(Set.of(29, 32), new HashSet<>(result));
    }

    @Test
    void testGenericValueBoolean() {
        List<Boolean> result = V().<Boolean>value("active").toList();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(true, result.get(0));
    }

    @Test
    void testGenericValueStringJsonPointer() {
        List<Integer> result = V().<Integer>value("/details/weight").toList();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(78, result.get(0));
    }

    @Test
    void testGenericValueJsonObjectJsonPointer() {
        List<JsonObject> result = V().<JsonObject>value("/details").toList();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("123456789", result.get(0).getString("document"));
    }

    @Test
    void testValues() {
        Map<Object, Object> map = V("v1").values(
            "name",
            "/details",
            "/details/weight",
            "w:/details/weight"
        ).next();
        Assertions.assertNotNull(map);
        Assertions.assertNull(map.get("id"));
        Assertions.assertEquals("marko", map.get("name"));
        Assertions.assertEquals(78, ((JsonObject)map.get("details")).getInteger("weight"));
        Assertions.assertEquals(78, map.get("weight"));
        Assertions.assertEquals(78, map.get("w"));
    }

    @Test
    void testValuesFromSelect() {
        Map<Object, Object> map = V().hasKey("name").as("person").outE("maintained").as("edge").inV().as("os")
            .select("person", "edge", "os")
            .values(
                "personName:/person/name",
                "edgeYearFrom:/edge/yearFrom",
                "osName:/os/name",
                "osActive:/os/active"
            ).next();

        Assertions.assertNotNull(map);
        Assertions.assertNull(map.get("id"));
        Assertions.assertEquals("josh", map.get("personName"));
        Assertions.assertEquals("2000", map.get("edgeYearFrom"));
        Assertions.assertEquals("linux", map.get("osName"));
        Assertions.assertEquals(true, map.get("osActive"));
    }

    @Test
    void testValueIsInteger() {
        List<Integer> result = V().<Integer>value("age").is(29).toList();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(29, result.get(0));
    }

    @Test
    void testValueIsIntegerPredicate() {
        List<Integer> result = V().<Integer>value("age").is(eq(29)).toList();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(29, result.get(0));
    }

    @Test
    void testValueIsString() {
        List<String> result = V().<String>value("/details/document").is("123456789").toList();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("123456789", result.get(0));

        result = V().has("/details/document", "123456789")
            .<String>value("/details/document").toList();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("123456789", result.get(0));
    }

    @Test
    void testValueIsStringPredicate() {
        List<String> result =
            V().<String>value("/details/document").is(startingWith("123")).toList();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("123456789", result.get(0));
    }

    @Test
    void testValuesIsIntString() {
        GraphTraversal<JsonObject, Map<Object, Object>> result = V().values("age", "lang").dedup().has("age", eq(29));
        Assertions.assertEquals(1, result.toList().size());

        result = result.has("lang", "go");
        Assertions.assertEquals(0, result.toList().size());
    }

    @Test
    void testValuesIsIntStringPredicate() {
        List<Map<Object, String>> result = V().<String>values("age", "lang").dedup()
            .has("age", eq(29))
            .has("lang", notContaining("java"))
            .toList();
        Assertions.assertEquals(0, result.size());
    }

    @Test
    void testValueOr() {
        @SuppressWarnings("unchecked")
        List<Integer> result = V("v1", "v4").<Integer>value("age")
            .<Integer>or(is(eq(29)), is(eq(32)), is(eq(100))).toList();
        Assertions.assertEquals(2, result.size());
    }

    @Test
    void testValuesOr() {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<Map> result = V().values("age", "lang").dedup() // [{"age":29}, {"age":32}, {"lang":"java"}]
            .<Map>or(has("age", eq(29)), has("lang", "java"))
            .toList();
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(29, result.get(0).get("age"));
        Assertions.assertEquals("java", result.get(1).get("lang"));
    }

    @Test
    void testCount() {
        Integer vertexCount = this.graph.getVertexCount();
        Long traversalCount = V().value("id").count().next();
        Assertions.assertEquals(vertexCount.longValue(), traversalCount.longValue());
    }

    @Test
    void testCountIs() {
        Long count = V().outE("created").count().is(2).next();
        Assertions.assertEquals(2l, count);

        count = V().outE("maintained").count().is(1).next();
        Assertions.assertEquals(1l, count);
    }

    @Test
    void testSelectVertex() {
        List<Map<String, Object>> result = V().as("person").out("created")
            .<String>value("name").as("softName")
            .select("person", "softName")
            .toList();
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("v4", ((JsonObject)result.get(0).get("person")).getString("id"));
        Assertions.assertEquals("lop", result.get(0).get("softName"));
        Assertions.assertEquals("v4", ((JsonObject)result.get(1).get("person")).getString("id"));
        Assertions.assertEquals("ripple", result.get(1).get("softName"));
    }

    @Test
    void testUnion() {
        @SuppressWarnings("unchecked")
        List<String> result = V()
            .value("name")
            .<String>union(is(eq("marko")), is(containing("nux"))).toList();
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("marko", result.get(0));
        Assertions.assertEquals("linux", result.get(1));
    }

    @Test
    void testTail() {
        GraphTraversal<JsonObject, String> traversal = V().hasType("person", "soft").value("id");
        Assertions.assertEquals(4, traversal.clone().count().next());

        List<String> result = traversal.toList();
        Assertions.assertEquals("v1", result.get(0));
        Assertions.assertEquals("v4", result.get(1));
        Assertions.assertEquals("v3", result.get(2));
        Assertions.assertEquals("v5", result.get(3));

        result = traversal.tail(2).toList();
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("v3", result.get(0));
        Assertions.assertEquals("v5", result.get(1));
    }

    @Test
    void testLimit() {
        GraphTraversal<JsonObject, String> traversal = V().hasType("person", "soft").<String>value("id");
        Assertions.assertEquals(4, traversal.clone().count().next());

        List<String> result = traversal.toList();
        Assertions.assertEquals("v1", result.get(0));
        Assertions.assertEquals("v4", result.get(1));
        Assertions.assertEquals("v3", result.get(2));
        Assertions.assertEquals("v5", result.get(3));

        result = traversal.limit(2).toList();
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("v1", result.get(0));
        Assertions.assertEquals("v4", result.get(1));
    }

    @Test
    public void testRange() {
        GraphTraversal<JsonObject, String> traversal = V().hasType("person", "soft").<String>value("id");
        Assertions.assertEquals(4, traversal.clone().count().next());

        List<String> result = traversal.toList();
        Assertions.assertEquals("v1", result.get(0));
        Assertions.assertEquals("v4", result.get(1));
        Assertions.assertEquals("v3", result.get(2));
        Assertions.assertEquals("v5", result.get(3));

        result = traversal.range(1, 3).toList();
        Assertions.assertEquals(List.of("v4", "v3"), result);
    }

    @Test
    public void testGroupCount() {
        List<Map<Object, Object>> result = E().<String>value("type")
            .group().by(count())
            .toList();
        assertEquals(1, result.size());
        assertEquals(Map.of("created", 2L, "maintained", 1L, "knows", 1L), result.get(0));
    }

    @Test
    void testValueGroup() {
        List<Map<Object, Object>> result = V().<String>value("name").group().toList();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(6, result.get(0).size());
    }

    @Test
    void testValuesGroup() {
        V().values("id", "name").group().next();
        V().values("id", "name").group().by("id").next();
        V().values("id", "name").group().by("id").by("name").next();
    }

    @SuppressWarnings("rawtypes")
    @Test
    void testCompositeKeyGroupBy() {
        List<Map<Object, Object>> result = V().hasKey("lang", "type").group().by("lang", "type").toList();
        Assertions.assertEquals(2, ((List)((Map)result.get(0)).get(result.get(0).keySet().iterator().next())).size());
    }

    @SuppressWarnings("rawtypes")
    @Test
    void testCompositeKeyGroupByBy() {
        List<Map<Object, Object>> result = V().hasKey("lang", "type").group().by("lang", "type").by("id", "name").toList();
        Assertions.assertEquals(2, ((List)((Map)result.get(0)).get(result.get(0).keySet().iterator().next())).size());
    }

    @Test
    void testValueOut() {
        List<String> result = V("v1").value("id").in().id().toList();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("root", result.get(0));
    }

    @Test
    void testId_out() {
        List<String> result = V("v1").id().in().id().toList();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("root", result.get(0));
    }

    @Test
    void testType_out() {
        List<String> result = V("v1").type().in().id().toList();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("root", result.get(0));
    }

    @Test
    void testValues_out() {
        List<String> result = V("v1").values("id", "type").in().id().toList();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("root", result.get(0));
    }

    @Test
    void testValues_afterSelect() {
        List<Map<Object, Object>> result = V("v4").values("id", "type").as("V")
            .out("maintained").as("C")
            .select("V", "C")
            .values("map:/V/id", "json:/C/id")
            .toList();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(2, result.get(0).size());
        Assertions.assertEquals("v6", result.get(0).get("json"));
        Assertions.assertEquals("v4", result.get(0).get("map"));
        // {json=v6, map=v4}
    }

    @Test
    void testBadValues_afterSelect() {
        List<Map<Object, Object>> result = V("v4").values("id", "type").as("V")
            .out("maintained").as("C")
            .select("V", "C")
            .values("map:/V/id/a", "json:/C/id", "x:/X")
            .toList();
//        print(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(1, result.get(0).size());
        Assertions.assertEquals("v6", result.get(0).get("json"));
        Assertions.assertNull(result.get(0).get("map"));
        Assertions.assertNull(result.get(0).get("x"));
        // {json=v6}, no key "map"
    }

    @Test
    void testValues_afterSelect_simple() {
        List<Map<Object, Object>> result = V("v4").values("id", "type").as("V")
            .out("maintained").as("C")
            .select("V", "C")
            .values("map:/V", "json:/C/id")
            .toList();
//        print(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(2, result.get(0).size());
        Assertions.assertEquals("v6", result.get(0).get("json"));
        Assertions.assertEquals("v4", JsonUtils.asMap(result.get(0).get("map")).get("id"));
    }

    @Test
    void testValueReplaceSimple() {
        // v1 == marko
        String result = V("v1").<String>value("name")
            .valueReplace("ko", "KO").next();
        Assertions.assertEquals("marKO", result);
    }

    @Test
    void testValueReplaceRegex() {
        // v1 == marko
        String result = V("v1").<String>value("name")
            .valueReplace("(ma)r(ko)", "$2r$1").next();
        Assertions.assertEquals("korma", result);
    }

}
        /*
        v1_marko -knows-> v4_josh -created->    v3_lop
                                  -created->    v5_ripple
                                  -maintained-> v6_linux
         */

