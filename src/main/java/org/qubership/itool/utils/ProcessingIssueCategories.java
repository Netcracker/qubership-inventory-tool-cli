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

package org.qubership.itool.utils;

import java.util.Set;

/**
 * Utility class providing standard processing issue category identifiers.
 */
public final class ProcessingIssueCategories {

    public static final String PARSING = "parsing";
    public static final String PROCESSING = "processing";
    public static final String VALIDATION = "validation";
    public static final String IO = "io";
    public static final String DATA_INTEGRITY = "data-integrity";

    private static final Set<String> KNOWN_CATEGORIES = Set.of(
            PARSING,
            PROCESSING,
            VALIDATION,
            IO,
            DATA_INTEGRITY
    );

    private ProcessingIssueCategories() {
        // Utility class
    }

    /**
     * @return immutable set of known category identifiers.
     */
    public static Set<String> knownCategories() {
        return KNOWN_CATEGORIES;
    }
}
