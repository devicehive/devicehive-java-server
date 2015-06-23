package com.devicehive.base.matcher;

import com.devicehive.exceptions.HiveException;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class HiveExceptionMatcher extends TypeSafeMatcher<HiveException> {

    private int code;

    private HiveExceptionMatcher(int code) {
        this.code = code;
    }

    public static HiveExceptionMatcher code(int code) {
        return new HiveExceptionMatcher(code);
    }

    @Override
    protected boolean matchesSafely(HiveException item) {
        return item.getCode() == code;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("expects code ").appendValue(code);
    }

    @Override
    protected void describeMismatchSafely(HiveException item, Description mismatchDescription) {
        mismatchDescription.appendText("was ").appendValue(item.getCode());
    }
}
