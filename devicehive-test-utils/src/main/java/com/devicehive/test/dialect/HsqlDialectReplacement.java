package com.devicehive.test.dialect;

/*
 * #%L
 * DeviceHive Java Server Common business logic
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


import org.hibernate.dialect.HSQLDialect;

/**
 * Fixes errors
 * ERROR o.h.tool.hbm2ddl.SchemaExport - HHH000389:
 * Unsuccessful: alter table access_key drop constraint FK_g41dixkcu4ku5xqxvr9vujke1
 */
public class HsqlDialectReplacement extends HSQLDialect {
    @Override
    public String getDropTableString( String tableName ) {
        // Append CASCADE to formatted DROP TABLE string
        final String superDrop = super.getDropTableString( tableName );
        return superDrop + " cascade";
    }
    @Override
    public boolean dropConstraints() {
        // Do not explicitly drop constraints, use DROP TABLE ... CASCADE
        return false;
    }
    }
