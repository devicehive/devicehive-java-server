package com.devicehive.dialect;

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
