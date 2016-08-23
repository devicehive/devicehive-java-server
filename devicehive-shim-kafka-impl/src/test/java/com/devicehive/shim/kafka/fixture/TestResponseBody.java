package com.devicehive.shim.kafka.fixture;

import com.devicehive.shim.api.Body;

public class TestResponseBody extends Body {

    private String responseBody;

    public TestResponseBody(String responseBody) {
        super("test_response");
        this.responseBody = responseBody;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }
}
