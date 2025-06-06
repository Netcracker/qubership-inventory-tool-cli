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

import org.qubership.itool.modules.gremlin2.Traversal;
import org.qubership.itool.modules.gremlin2.step.util.Profile;

import java.util.List;

public class ProfileStep<S> extends ReducingBarrierStep<S, Profile> {

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    public ProfileStep(Traversal.Admin traversal) {
        super(traversal);
        Traversal.Admin rootTraversal = fetchRootTraversal();
        rootTraversal.getProfile().start();
    }

    @Override
    protected Profile projectTraversers(List previousTraversers) {
        Traversal.Admin rootTraversal = fetchRootTraversal();
        return rootTraversal.getProfile();
    }

}
