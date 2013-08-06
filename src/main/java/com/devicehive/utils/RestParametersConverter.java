package com.devicehive.utils;

//TODO:javadoc
public class RestParametersConverter {

    private static final String SORT_ASCENDING = "ASC";

    private static final String SORT_DESCENDING = "DESC";

    /**
     * Check whatever provided string is ASC or DESC. If specified string is null, will return DESC
     *
     * @param sort string to check
     * @return true if @param{sort} is ASC, false if DESC or null, null otherwise
     */
    public static Boolean isSortAsc(String sort) {

        if (sort == null) {
            return false;
        }

        if (SORT_ASCENDING.equalsIgnoreCase(sort)) {
            return true;
        }

        if (SORT_DESCENDING.equalsIgnoreCase(sort)) {
            return false;
        }

        return null;
    }
}
