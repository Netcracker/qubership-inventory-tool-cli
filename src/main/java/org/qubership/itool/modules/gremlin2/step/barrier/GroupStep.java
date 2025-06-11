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

package org.qubership.itool.modules.gremlin2.step.barrier;

import org.qubership.itool.modules.gremlin2.DefaultTraverser;
import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.Traverser;
import org.qubership.itool.modules.gremlin2.step.AbstractStep;
import org.qubership.itool.modules.gremlin2.step.ByModulating;
import org.qubership.itool.modules.gremlin2.util.ValueHelper;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupStep<S, K, V> extends ReducingBarrierStep<S, Map<K, V>> implements ByModulating {

    private String[] modulateBy;
    private String[] modulateByBy;
    private Traversal.Admin modulateByTraversal;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "("
            + (modulateBy == null ? "": modulateBy)
            + (modulateByBy == null ? "": "," + modulateByBy)
            + (modulateByTraversal == null ? "": "traversal")
            + ")";
    }

    @Override
    public void clear() {
        clearTraversal(modulateByTraversal);
    }

    public GroupStep(Traversal.Admin traversal) {
        super(traversal);
    }

    @Override
    protected Map<K, V> projectTraversers(List<Traverser<S>> previousTraversers) {
        Map<K, V> result = new HashMap<>();
        Map<K, V> resultBy = null;
        Map<K, V> resultByBy = null;

        // ===================================================================
        for (Traverser<S> traverser : previousTraversers) {
            S obj = traverser.get();
            List objList = (List) result.get(obj);
            if (objList == null) {
                objList = new ArrayList();
                result.put((K) obj, (V) objList);
            }
            objList.add(obj);
        }

        // ===================================================================
        if (this.modulateBy != null) {
            resultBy = new HashMap<>();
            for (K resultKey : result.keySet()) {
                Object byKey = extractByValue(resultKey, this.modulateBy);
                if (byKey == null) {
                    continue;
                }
                List resultByList = (List)resultBy.get(byKey);
                for (Object obj : ((List)result.get(resultKey))) {
                    if (resultByList == null) {
                        resultByList = new ArrayList();
                        resultBy.put((K)byKey, (V)resultByList);
                    }
                    resultByList.add(resultKey);
                }
            }
        }

        // ===================================================================
        if (this.modulateByBy != null) {
            resultByBy = new HashMap<>();
            for (K resultKey : resultBy.keySet()) {
                List resultList = (List)resultBy.get(resultKey);
                List newList = new ArrayList();
                resultByBy.put(resultKey, (V)newList);
                for (Object obj : resultList) {
                    Object value = extractByValue(obj, this.modulateByBy);
                    if (value != null) {
                        newList.add(value);
                    }
                }
            }
        }

        // ===================================================================
        if (this.modulateByTraversal != null) {
            resultByBy = new HashMap<>();
            if (resultBy == null) {
                resultBy = result;
            }
            for (K resultKey : resultBy.keySet()) {
                List resultList = (List)resultBy.get(resultKey);
                List<Traverser.Admin<S>> trList = new ArrayList<>();
                for (Object obj : resultList) {
                    trList.add(new DefaultTraverser<>((S)obj));
                }
//                Traversal.Admin cloneTraversal = this.modulateByTraversal.clone();
                Traversal.Admin cloneTraversal = prepareInnerTraversal(this.modulateByTraversal, trList);
                List modulateList = cloneTraversal.toList();
                if (modulateList.size() > 1) {
                    resultByBy.put(resultKey, (V)modulateList);
                } else if (modulateList.size() == 1) {
                    resultByBy.put(resultKey, (V)modulateList.get(0));
                }
            }
        }

        if (resultByBy != null) {
            return resultByBy;
        }
        if (resultBy != null) {
            return resultBy;
        }
        return result;
    }

    private Object extractByValue(Object obj, String ... modulateBy) {
        Object result = null;
        if (modulateBy.length == 1) {
            result = extractByValue(obj, modulateBy[0]);
        } else {
            result = new HashMap();
            for (int i=0 ; i<modulateBy.length ; i++) {
                ((Map)result).put(modulateBy[i], extractByValue(obj, modulateBy[i]));
            }
        }
        return result;
    }

    private Object extractByValue(Object obj, String modulateBy) {
        Object result;
        if (obj instanceof JsonObject) {
            JsonObject json = (JsonObject)obj;
            result = ValueHelper.getObjectValue(modulateBy, json);

        } else if (obj instanceof Map) {
            Map map = (Map) obj;
            result = map.get(modulateBy);

        } else {
            throw new UnsupportedOperationException("Modulation .by() not supported for " + obj.getClass().getSimpleName());
        }
        return result;
    }

    @Override
    public void modulateBy(String string) throws UnsupportedOperationException {
        if (this.modulateBy == null) {
            this.modulateBy = new String[] { string };

        } else if (this.modulateByBy == null) {
            this.modulateByBy = new String[] { string };

        } else {
            throw new UnsupportedOperationException("only two .by() modulation allowed for GroupStep()");
        }
    }

    @Override
    public void modulateBy(String ... args) throws UnsupportedOperationException {
        if (this.modulateBy == null) {
            this.modulateBy = args;

        } else if (this.modulateByBy == null) {
            this.modulateByBy = args;

        } else {
            throw new UnsupportedOperationException("only two .by() modulation allowed for GroupStep()");
        }
    }

    @Override
    public void modulateBy(Traversal traversal) throws UnsupportedOperationException {
        this.modulateByTraversal = (Traversal.Admin) traversal;
    }

    @Override
    public AbstractStep<S, Map<K, V>> clone() {
        GroupStep clone =  (GroupStep)super.clone();
        clone.modulateBy = this.modulateBy;
        clone.modulateByBy = this.modulateByBy;
        clone.modulateByTraversal = this.modulateByTraversal;
        return clone;
    }

}
