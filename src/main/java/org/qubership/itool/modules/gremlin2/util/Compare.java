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

package org.qubership.itool.modules.gremlin2.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Compare implements BiPredicate<Object, Object> {

    eq {
        @Override
        public boolean test(Object first, Object second) {
            if (null == first) {
                return null == second;
            } else if (bothAreNumber(first, second)) {
                return compareNumber((Number) first, (Number) second) == 0;
            } else if (second instanceof List) {
                List list = (List)second;
                if (list.size() == 1) {
                    return list.get(0).equals(first);
                } else {
                    return false;
                }
            } else {
                return first.equals(second);
            }
        }

        @Override
        public Compare negate() {
            return neq;
        }
    },

    neq {
        @Override
        public boolean test(Object first, Object second) {
            return !eq.test(first, second);
        }

        @Override
        public Compare negate() {
            return eq;
        }
    },

    lt {
        @Override
        public boolean test(Object first, Object second) {
            return null == first ? null == second
                : (bothAreNumber(first, second) ? compareNumber((Number) first, (Number) second) < 0
                : first.equals(second));
        }

        @Override
        public Compare negate() {
            return gte;
        }
    },

    lte {
        @Override
        public boolean test(Object first, Object second) {
            return null == first ? null == second
                : (bothAreNumber(first, second) ? compareNumber((Number) first, (Number) second) <= 0
                : first.equals(second));
        }

        @Override
        public Compare negate() {
            return gt;
        }
    },

    gt {
        @Override
        public boolean test(Object first, Object second) {
            return null == first ? null == second
                : (bothAreNumber(first, second) ? compareNumber((Number) first, (Number) second) > 0
                : first.equals(second));
        }

        @Override
        public Compare negate() {
            return lte;
        }
    },

    gte {
        @Override
        public boolean test(Object first, Object second) {
            return null == first ? null == second
                : (bothAreNumber(first, second) ? compareNumber((Number) first, (Number) second) >= 0
                : first.equals(second));
        }

        @Override
        public Compare negate() {
            return lt;
        }
    },

    lteVersion {
        @Override
        public boolean test(Object first, Object second) {
            if (!bothAreVersion(first, second)) {
                return false;
            }
            String firstVersion = (String) first;
            String secondVersion = (String) second;
            Matcher firstMatcher = versionPattern.matcher(firstVersion);
            Matcher secondMatcher = versionPattern.matcher(secondVersion);
            firstMatcher.matches();
            secondMatcher.matches();
            int firstGroupCount = firstMatcher.groupCount();
            int secondGroupCount = secondMatcher.groupCount();
            int groupCount = (firstGroupCount >= secondGroupCount) ? firstGroupCount : secondGroupCount;
            for (int i=1 ; i< groupCount ; i++) {
                if (firstMatcher.group(i) == null || secondMatcher.group(i) == null) {
                    continue;
                }
                int firstValue = Integer.parseInt(firstMatcher.group(i));
                int secondValue = Integer.parseInt(secondMatcher.group(i));
                if (firstValue == secondValue) {
                    continue;
                }
                return secondValue > firstValue;
            }
            return true;
        }

        @Override
        public Compare negate() {
            return gteVersion;
        }
    },

    gteVersion {
        @Override
        public boolean test(Object first, Object second) {
            if (!bothAreVersion(first, second)) {
                return false;
            }
            String firstVersion = (String) first;
            String secondVersion = (String) second;
            Matcher firstMatcher = versionPattern.matcher(firstVersion);
            Matcher secondMatcher = versionPattern.matcher(secondVersion);
            firstMatcher.matches();
            secondMatcher.matches();
            int firstGroupCount = firstMatcher.groupCount();
            int secondGroupCount = secondMatcher.groupCount();
            int groupCount = (firstGroupCount >= secondGroupCount) ? firstGroupCount : secondGroupCount;
            for (int i=1 ; i< groupCount ; i++) {
                int firstValue = Integer.parseInt(firstMatcher.group(i));
                int secondValue = Integer.parseInt(secondMatcher.group(i));
                if (firstValue == secondValue) {
                    continue;
                }
                return secondValue < firstValue;
            }
            return true;
        }

        @Override
        public Compare negate() {
            return lteVersion;
        }
    },

    within {
        @Override
        public boolean test(Object first, Object second) {
            if (second instanceof List) {
                List secondList = (List)second;
                return secondList.contains(first);
            }
            return false;
        }

        @Override
        public Compare negate() {
            return without;
        }

    },

    without {
        @Override
        public boolean test(Object first, Object second) {
            if (second instanceof List) {
                List secondList = (List)second;
                return !secondList.contains(first);
            }
            return false;
        }

        @Override
        public Compare negate() {
            return within;
        }

    },

    exists {

        @Override
        public boolean test(Object first, Object second) {
            return (first != null);
        }

        @Override
        public Compare negate() {
            return isNull;
        }

    },

    isNull {

        @Override
        public boolean test(Object first, Object second) {
            return (first == null);
        }

        @Override
        public Compare negate() {
            return exists;
        }

    },

    containing {
        @Override
        public boolean test(Object first, Object second) {
            if (bothAreString(first, second)) {
                return ((String)first).contains((String)second);
            } else if (first instanceof JsonArray) {
                return ((JsonArray)first).contains(second);
            }
            return false;
        }

        @Override
        public Compare negate() {
            return notContaining;
        }
    },

    notContaining {
        @Override
        public boolean test(Object first, Object second) {
            if (bothAreString(first, second)) {
                return !((String)first).contains((String)second);
            } else if (first instanceof JsonArray) {
                return !((JsonArray)first).contains(second);
            }
            return false;
        }

        @Override
        public Compare negate() {
            return containing;
        }
    },

    startingWith {
        @Override
        public boolean test(Object first, Object second) {
            if (bothAreString(first, second)) {
                return ((String)first).startsWith((String)second);
            }
            return false;
        }
        @Override
        public Compare negate() {
            return notStartingWith;
        }
    },

    notStartingWith {
        @Override
        public boolean test(Object first, Object second) {
            if (bothAreString(first, second)) {
                return !((String)first).startsWith((String)second);
            }
            return false;
        }
        @Override
        public Compare negate() {
            return startingWith;
        }
    },

    endingWith {
        @Override
        public boolean test(Object first, Object second) {
            if (bothAreString(first, second)) {
                return ((String)first).endsWith((String)second);
            }
            return false;
        }
        @Override
        public Compare negate() {
            return notEndingWith;
        }
    },

    notEndingWith {
        @Override
        public boolean test(Object first, Object second) {
            if (bothAreString(first, second)) {
                return !((String)first).endsWith((String)second);
            }
            return false;
        }
        @Override
        public Compare negate() {
            return endingWith;
        }
    }
    ;

    // ======================================================================================
    // ======================================================================================
    // ======================================================================================

    public static int compareNumber(Number first, Number second) {
        long firstLong = first.longValue();
        long secondLong = second.longValue();
        if (firstLong == secondLong) {
            // XXX Does not support floating-point numbers, though SumStep supports them
            return 0;
        } if (firstLong > secondLong) {
            return 1;
        }
        return -1;
    }

    public static boolean bothAreVersion(Object first, Object second) {
        return isVersion(first) && isVersion(second);
    }

    public static boolean bothAreNumber(Object first, Object second) {
        return isNumber(first) && isNumber(second);
    }

    public static boolean bothAreString(Object first, Object second) {
        return isString(first) && isString(second);
    }

    public static boolean isString(Object obj) {
        return (obj instanceof String);
    }

    public static boolean isNumber(Object number) {
        if (number instanceof Integer) {
            return true;
        }
        if (number instanceof Long) {
            return true;
        }
        // XXX Does not consider floating-point numbers, though SumStep supports them
        return false;
    }

    private static final Pattern versionPattern = Pattern.compile("^\\D*(\\d+)(?:[\\._](\\d+))?(?:[\\._](\\d+))?(?:[\\._](\\d+))?.*$");

    public static boolean isVersion(Object version) {
        if (!(version instanceof String)) {
            return false;
        }
        String ver = (String) version;
        return versionPattern.matcher(ver).matches();
    }

    public static boolean bothAreJsonObject(Object first, Object second) {
        if (first instanceof JsonObject) {
            if (second instanceof JsonObject) {
                return true;
            }
        }
        return false;
    }

    public static int compareJsonObject(JsonObject first, JsonObject second) {
        return first.getString("id").compareTo(second.getString("id"));
    }

}
