package com.jklis.test.thread.efficiency.record;

public record RequestResult(boolean isValid, int statusCode, String body) {

    private static final int REQUEST_DID_NOT_OCCURED_STS_CD = 0;

    public RequestResult(boolean isValid, String body) {
        this(isValid, REQUEST_DID_NOT_OCCURED_STS_CD, body);
    }
}
