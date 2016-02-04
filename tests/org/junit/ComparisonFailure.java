package org.junit;

/**
 * Fake ComparisonFailure class since JUnit isn't installed.
 * TODO replace with real JUnit jar.
 */
public class ComparisonFailure extends AssertionError {
    private static final long serialVersionUID = 1L;

    public ComparisonFailure(String message, String expected, String actual) {
        super(message + " | Expected: " + expected + " | Actual: " + actual);
    }
}
