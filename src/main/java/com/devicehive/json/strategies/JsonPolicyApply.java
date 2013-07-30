package com.devicehive.json.strategies;


import javax.enterprise.util.AnnotationLiteral;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface JsonPolicyApply {

    JsonPolicyDef.Policy value();

    public static class JsonPolicyApplyLiteral extends AnnotationLiteral<JsonPolicyApply>
            implements JsonPolicyApply{

        private JsonPolicyDef.Policy policy;

        public JsonPolicyApplyLiteral(JsonPolicyDef.Policy policy){
            this.policy = policy;
        }

        @Override
        public JsonPolicyDef.Policy value() {
            return policy;
        }
    }
}
