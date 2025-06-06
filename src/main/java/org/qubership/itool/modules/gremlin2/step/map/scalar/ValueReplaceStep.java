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

package org.qubership.itool.modules.gremlin2.step.map.scalar;

import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import io.vertx.core.json.JsonObject;

import java.util.regex.Pattern;

public class ValueReplaceStep<S extends JsonObject> extends ScalarMapStep<S, String>{

    private String regex;
    private String replacement;

    public ValueReplaceStep(final Traversal.Admin traversal, String regex, String replacement) {
        super(traversal);
        this.regex = regex;
        this.replacement = replacement;
    }

    @Override
    protected String map(final Traverser.Admin<S> traverser) {
        Object obj = traverser.get();

        if (obj instanceof String) {
            String str = (String) obj;
            return Pattern.compile(regex).matcher(str).replaceFirst(replacement);
//            return str.replaceFirst(this.regex, this.replacement);
        }

        return null;
    }

    @Override
    protected Traverser<String> generateTraverser(Traverser.Admin<S> previousTraverser, String value) {
        Traverser<String> traverser = previousTraverser.split(
            (previousTraverser.getSource() != null) ? previousTraverser.getSource() : null,
            value, this);
        return traverser;
    }

}
