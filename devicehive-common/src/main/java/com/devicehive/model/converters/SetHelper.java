package com.devicehive.model.converters;

/*
 * #%L
 * DeviceHive Common Module
 * %%
 * Copyright (C) 2016 - 2017 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SetHelper {
    
    public static Set<String> toStringSet(String stringCsv) {
        return Optional.ofNullable(StringUtils.split(stringCsv, ','))
                .map(Arrays::asList)
                .map(list -> list.stream().collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
    }

    public static Set<Long> toLongSet(String stringCsv) {
        return Optional.ofNullable(StringUtils.split(stringCsv, ','))
                .map(Arrays::asList)
                .map(list -> list.stream().map(str -> Long.valueOf(str)).collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
    }
}
