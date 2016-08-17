package com.devicehive.json.strategies;

/*
 * #%L
 * DeviceHive Common Dao Interfaces
 * %%
 * Copyright (C) 2016 DataArt
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


import javax.enterprise.util.AnnotationLiteral;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonPolicyApply {

    JsonPolicyDef.Policy value();

    class JsonPolicyApplyLiteral extends AnnotationLiteral<JsonPolicyApply>
        implements JsonPolicyApply {


        private static final long serialVersionUID = 7838737655418173629L;
        private JsonPolicyDef.Policy policy;

        public JsonPolicyApplyLiteral(JsonPolicyDef.Policy policy) {
            this.policy = policy;
        }

        @Override
        public JsonPolicyDef.Policy value() {
            return policy;
        }
    }
}
