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

package org.qubership.itool.modules.query;

import org.qubership.itool.modules.graph.BasicGraph;
import org.qubership.itool.modules.gremlin2.GremlinException;
import org.qubership.itool.modules.query.converter.ResultConverter;
import org.qubership.itool.modules.query.converter.ToTextConverter;

import groovy.lang.Binding;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;

import java.util.Properties;

public class QueryExecutor {

    private GroovyShell shell;

    private ResultConverter<?> converter = new ToTextConverter(); // By default ready for CLI

    private Properties converterProps;

    public QueryExecutor(BasicGraph graph) {

        Binding binding = new Binding();
        binding.setProperty("traversal", graph.traversal());
        this.shell = new GroovyShell(binding);

        this.converterProps = new Properties();

        // Set default value
        converterProps.put("view.json", "compact");
        converterProps.put("view.map", "compact");
        converterProps.put("result.limit", -1);
    }

    public void setConverter(ResultConverter<?> converter) {
        this.converter = converter;
    }

    public Object executeGremlinQuery(String query) throws GremlinException {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("import io.vertx.core.json.JsonObject;\n");
            builder.append("import org.qubership.itool.modules.gremlin2.graph.GraphTraversal;\n");
            builder.append("import org.qubership.itool.modules.gremlin2.Path;\n");
            builder.append("import org.qubership.itool.modules.gremlin2.structure.Direction\n");
            builder.append("import org.qubership.itool.modules.gremlin2.P;\n");
            builder.append("import org.qubership.itool.modules.gremlin2.graph.__\n");
            builder.append("import org.qubership.itool.modules.gremlin2.util.Order\n");

            builder.append("import static org.qubership.itool.modules.gremlin2.P.*;\n");
            builder.append("import static org.qubership.itool.modules.gremlin2.graph.__.*;\n");
            builder.append("import static org.qubership.itool.modules.gremlin2.util.Order.*;\n");
            builder.append("import static org.qubership.itool.modules.gremlin2.structure.MapElement.*;\n");
            builder.append("import static org.qubership.itool.modules.gremlin2.util.StringSplitter.*;\n");
            builder.append("\n");

            builder.append("result = traversal").append(query);
            this.shell.evaluate(builder.toString());

            return this.shell.getProperty("result");

        } catch (GroovyRuntimeException e) {
            throw new GremlinException("Execution failed. Reason: " + e.getMessage());
        }
    }

    public Object executeAndConvert(String query) {
        Object result = executeGremlinQuery(query);
        return convertResult(result);
    }

    protected Object convertResult(Object result) {
        converter.setProperties(converterProps);
        return converter.convert(result);
    }

}