package com.devicehive.json.strategies;


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
