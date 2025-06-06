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

package org.qubership.itool.modules.query.converter;

import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.step.filter.RangeLimitStep;
import io.vertx.core.json.JsonObject;

import java.util.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ToTextConverter implements ResultConverter<String> {

    private Properties props;

    @Override
    public void setProperties(Properties properties) {
        this.props = properties;
    }

    @Override
    public String supportType() {
        return "text";
    }

    @Override
    public String convert(Object gremlinResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("\"Result\":\n");
        int count = 0;

        if (gremlinResult instanceof Traversal) {
            if ((Integer)this.props.getOrDefault("result.limit", -1) != -1) {
                Traversal.Admin<?,?> traversal = (Traversal.Admin<?, ?>) gremlinResult;
                traversal.addStep(
                        new RangeLimitStep<>(
                                traversal,
                                Optional.empty(),
                                Optional.of((Integer)this.props.get("result.limit")))
                );
            }
            gremlinResult = ((Traversal<?, ?>) gremlinResult).toList();
        }

        if (gremlinResult instanceof List) {
            List list = (List)gremlinResult;
            if (list.size() == 1 && (list.get(0) instanceof Map)) {
                convertMap(builder, (Map)list.get(0));
                count = ((Map)list.get(0)).size();
            } else {
                convertList(builder, (List) gremlinResult);
                count = ((List) gremlinResult).size();
            }

        } else if (gremlinResult instanceof Map) {
            convertMap(builder, (Map)gremlinResult);
            count = ((Map)gremlinResult).size();

        } else if (gremlinResult instanceof JsonObject) {
            convertJsonObject(builder, (JsonObject)gremlinResult);
            count = 1;

        } else {
            convertScalar(builder, gremlinResult);
            count = 1;
        }

        builder.append("\n,\"Total\": ").append(count).append("\n");
        return builder.toString();
    }

    private void convertList(StringBuilder builder, List list) {
        if (list.size() != 0) {
            if ((list.get(0) instanceof String) || (list.get(0) instanceof Number)) {
                flatArray(builder, list);
                return;
            }
        }

        boolean isMarkdown = this.props.getProperty("view.map", "full").equals("markdown");
        if (isMarkdown && list.get(0) instanceof Map) {
            Set headerSet = new LinkedHashSet();
            for (Object row : list) {
                if (row instanceof Map) {
                    Iterator iter = ((Map)row).keySet().iterator();
                    while (iter.hasNext()) {
                        headerSet.add(iter.next());
                    }
                }
            }

            builder.append("||");
            for (Object key : headerSet) {
                builder.append(key).append("||");
            }
            builder.append("\n");

            for (Object row : list) {
                builder.append("|");
                if (row instanceof Map) {
                    Map map = (Map)row;
                    for (Object key : headerSet) {
                        Object value = map.get(key);
                        builder.append(value).append("|");
                    }
                } else {
                    builder.append("not a Map|"); // TODO wrong column number
                }
                builder.append("\n");
            }

            return;
        }

        builder.append("[");
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            Object obj = iter.next();
            if (obj instanceof List) {
                convertList(builder, (List)obj);

            } else if (obj instanceof Map) {
                convertMap(builder, (Map)obj);

            } else if (obj instanceof JsonObject) {
                convertJsonObject(builder, (JsonObject)obj);

            } else {
                convertScalar(builder, obj);
            }
            builder.append("\n");
            if (iter.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append("]");
    }

    private void convertMap(StringBuilder builder, Map map) {
        boolean isCompact = this.props.getProperty("view.map", "full").equals("compact");
        boolean isMarkdown = this.props.getProperty("view.map", "full").equals("markdown");
        isCompact = isFlatMap(map);

        if (isMarkdown) {
            Iterator iter = map.keySet().iterator();
            builder.append("||");
            while (iter.hasNext()) {
                Object key = iter.next();
                builder.append(key).append("||");
            }
            builder.append("\n").append("|");
            while (iter.hasNext()) {
                Object value = map.get(iter.next());
                builder.append(value).append("|");
            }
            builder.append("\n");
            return;
        }

        if (isCompact) {
            Iterator iter = map.keySet().iterator();
            builder.append("{");
            while (iter.hasNext()) {
                Object key = iter.next();
                builder.append(key).append(" = ");
                Object value = map.get(key);
                if (value instanceof List) {
                    flatArray(builder, (List)value);
                } else if (value instanceof JsonObject) {
                    convertJsonObject(builder, (JsonObject) value);
                } else {
                    convertScalar(builder, value);
                }
                if (iter.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append("}");

        } else {
            boolean isFirst = true;
            Iterator iter = map.keySet().iterator();
            builder.append("{\n");
            while (iter.hasNext()) {
                Object key = iter.next();
                if (isFirst) {
                    builder.append("  ");
                    isFirst = false;
                } else {
                    builder.append(", ");
                }
                builder.append(key).append(" = ");
                Object value = map.get(key);
                if (value instanceof List) {
                    flatArray(builder, (List)value);
                } else if (value instanceof JsonObject) {
                    convertJsonObject(builder, (JsonObject) value);
                } else {
                    convertScalar(builder, value);
                }
                builder.append("\n");
            }
            builder.append("}");
        }
    }

    private void convertJsonObject(StringBuilder builder, JsonObject json) {
        if (this.props.getProperty("view.json", "full").equals("compact")) {
            JsonObject newJson = new JsonObject();
            newJson.put("id", json.getString("id"));
            newJson.put("type", json.getString("type"));
            newJson.put("name", json.getString("name"));
            json = newJson;
        }
        builder.append(json.toString());
    }

    private void convertScalar(StringBuilder builder, Object scalar) {
        builder.append(scalar);
//        if (scalar instanceof String) {
//            builder.append("\"").append(scalar).append("\"");
//        } else {
//            builder.append(scalar);
//        }
    }


    // ====================================================================

    private void flatArray(StringBuilder builder, List list) {
        builder.append("[");
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            Object value = iter.next();
            if (value instanceof List) {
                flatArray(builder, (List) value);
            } else if (value instanceof Map) {
                flatMap(builder, (Map) value);
            } else if (value instanceof JsonObject) {
                convertJsonObject(builder, (JsonObject) value);
            } else {
                convertScalar(builder, value);
            }
            if (iter.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append("]");
    }

    private void flatMap(StringBuilder builder, Map map) {
        builder.append("{");
        Iterator iter = map.keySet().iterator();
        while (iter.hasNext()) {
            Object key = iter.next();
            builder.append(key).append(" = ");
            Object value = map.get(key);
            if (value instanceof List) {
                flatArray(builder, (List) value);
            } else if (value instanceof Map) {
                flatMap(builder, (Map) value);
            } else if (value instanceof JsonObject) {
                convertJsonObject(builder, (JsonObject) value);
            } else {
                convertScalar(builder, value);
            }

            if (iter.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append("}");
    }

    // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    // -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    private boolean isFlatMap(Map map) {
        boolean result = true;
        for (Object key : map.keySet()) {
            Object value = map.get(key);
            if ((value instanceof List) || (value instanceof Map)) {
                result = false;
                break;
            }
        }
        return result;
    }

}
