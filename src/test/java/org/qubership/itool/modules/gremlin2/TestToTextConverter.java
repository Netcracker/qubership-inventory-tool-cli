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

import org.qubership.itool.modules.query.converter.ResultConverter;
import org.qubership.itool.modules.query.converter.ToTextConverter;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestToTextConverter {

    Properties props;
    JsonObject jsonObject;
    ResultConverter<String> resultConverter;

    @BeforeAll
    public void setup() {
        this.props = new Properties();

        this.resultConverter = new ToTextConverter();
        this.resultConverter.setProperties(props);

        this.jsonObject = new JsonObject()
            .put("id", "id:1").put("name", "jsonName").put("type", "object")
            .put("birthYear", 2020)
            .put("birthMonth", 12)
            .put("birthDay", 3)
            .put("details", new JsonObject().put("domain", "global"));
    }

    @BeforeEach
    public void clear() {
        props.put("view.json", "compact");
        props.put("view.map", "compact");
        props.put("result.limit", -1);
    }

    @Test
    void testScalar() {
        String result = resultConverter.convert(14l);
        System.out.println(result);
    }

    @Test
    void testJsonObject_simple() {
        String result = resultConverter.convert(jsonObject);
        System.out.println(result);
    }

    @Test
    void testJsonObject_full() {
        props.put("view.json", "full");
        String result = resultConverter.convert(jsonObject);
        System.out.println(result);
    }

    @Test
    void testMapCompact_String_String() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "a1");
        map.put("b", "b1");
        String result = resultConverter.convert(map);
        System.out.println(result);
    }

    @Test
    void testMapExpand_String_String() {
        props.put("view.map", "expand");
        Map<String, String> map = new HashMap<>();
        map.put("a", "a1");
        map.put("b", "b1");
        String result = resultConverter.convert(map);
        System.out.println(result);
    }

    @Test
    void testList_String() {
        List list = new ArrayList();
        list.add("a");
        list.add("b");
        String result = resultConverter.convert(list);
        System.out.println(result);
    }

    @Test
    void testMap_Sting_Array_String() {
        List result = new ArrayList();
        Map map = new HashMap();
        List value1 = new ArrayList();
        List value2 = new ArrayList();

        result.add(map);
        map.put("a", value1);
        map.put("b", value2);
        value1.add("a1");
        value1.add("a2");
        value1.add("a3");
        value2.add("b1");

        String resultStr = resultConverter.convert(result);
        System.out.println(resultStr);
    }
}
