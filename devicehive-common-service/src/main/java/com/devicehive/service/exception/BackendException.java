package com.devicehive.service.exception;

/*
 * #%L
 * DeviceHive Frontend Logic
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

public class BackendException extends Exception {
    private static final long serialVersionUID = -8016952729321715123L;


    private int errorCode;

    public BackendException(int errorCode) {
        this.errorCode = errorCode;
    }

    public BackendException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BackendException(String message, Throwable cause, int errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public BackendException(Throwable cause, int errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    public BackendException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, int errorCode) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
