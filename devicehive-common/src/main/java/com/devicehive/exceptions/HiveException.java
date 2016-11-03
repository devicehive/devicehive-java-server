package com.devicehive.exceptions;

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


import com.devicehive.configuration.Messages;

import javax.servlet.http.HttpServletResponse;

public class HiveException extends RuntimeException {

    private static final long serialVersionUID = 6413354755792688308L;

    private Integer code = null;

    public Integer getCode() {
        return code;
    }

    public HiveException(String message, Throwable cause) {
        this(message, cause, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    public HiveException(String message) {
        this(message, null, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    public HiveException(String message, Integer code) {
        this(message, null, code);
    }

    public HiveException(String message, Throwable cause, Integer code) {
        super(message, cause);
        this.code = code != null ? code : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    public static HiveException fatal() {
        return new HiveException(Messages.INTERNAL_SERVER_ERROR);
    }

}
