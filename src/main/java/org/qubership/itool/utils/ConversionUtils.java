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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConversionUtils {

    public static final Pattern BYTES_PATTERN = Pattern.compile("^(\\d+)(\\D+)$");

    public static long convertToBytes(String orig) {
        long result = -1;
        if (orig == null) {
            return result;
        }
        Matcher matcher = BYTES_PATTERN.matcher(orig.toLowerCase());
        if (matcher.matches()) {
            String number = matcher.group(1);
            String unit = matcher.group(2);
            result = Long.valueOf(number);
            if ("k".equals(unit) || "kb".equals(unit)) {
                result = result * 1000;
            } else if ("m".equals(unit) || "mb".equals(unit)) {
                result = result * 1000 * 1000;
            } else if ("g".equals(unit) || "gb".equals(unit)) {
                result = result * 1000 * 1000 * 1000;
            } else if ("mi".equals(unit)) {
                result = result * 1024 * 1024;
            } else if ("gi".equals(unit)) {
                result = result * 1024 * 1024 * 1024;
            } else {
                return -1;
            }
        }
        return result;
    }

    public static Long convertToMillicores(Object cpuLimit) {
        if (cpuLimit instanceof String) {
            if (((String) cpuLimit).contains("m")) {
                return Long.parseLong(((String) cpuLimit).replace("m", ""));
            } else {
                return Long.parseLong(((String) cpuLimit)) * 1000;
            }
        } else if (cpuLimit instanceof Integer) {
            return (long) ((Integer) cpuLimit * 1000);
        } else {
            return null;
        }
    }
}
