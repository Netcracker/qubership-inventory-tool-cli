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

package org.qubership.itool.modules.gremlin2.graph.lambda;


import org.qubership.itool.modules.gremlin2.graph.DefaultGraphTraversal;


public class TrueTraversal<S> extends AbstractLambdaTraversal<S, S> {

    private static final TrueTraversal INSTANCE = new TrueTraversal();

    private TrueTraversal() {
        this.bypassTraversal = new DefaultGraphTraversal<>();
    }

    public static <S> TrueTraversal<S> instance() {
        return INSTANCE;
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public TrueTraversal<S> clone() {
        return INSTANCE;
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof TrueTraversal);
    }

}
