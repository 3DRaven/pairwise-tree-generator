/*******************************************************************************
 * Copyright 2021 Renat Eskenin
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.anyqn.lib;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class Properties {
    private List<Restriction> minRestrictions = new ArrayList<>();
    private List<Restriction> maxRestrictions = new ArrayList<>();
    private Set<String> objects = new HashSet<>();

    public int getMaxRestriction(@NonNull final String path) {
        return maxRestrictions.stream().filter(r -> r.getFieldPath().equals(path)).findFirst()
                .map(Restriction::getValue).orElse(Integer.MAX_VALUE);
    }

    public int getMinRestriction(@NonNull final String path) {
        return minRestrictions.stream().filter(r -> r.getFieldPath().equals(path)).findFirst()
                .map(Restriction::getValue).orElse(0);
    }

    public boolean isObject(@NonNull final String path) {
        return objects.contains(path);
    }

    @Data
    @NoArgsConstructor
    public static class Restriction {
        @NonNull
        private String fieldPath;
        private int value;

    }

}
