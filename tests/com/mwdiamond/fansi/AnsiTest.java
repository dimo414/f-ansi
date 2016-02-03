package com.mwdiamond.fansi;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Basic, change-detector style unit tests
 */
public class AnsiTest {
    private static final String helloWorld = "Hello World";
    private AnsiForTests ansiForTests;

    @BeforeMethod
    private void flushAnsi() {
        ansiForTests = new AnsiForTests();
    }

    private Ansi ansi() {
        return ansiForTests.ansi();
    }

    @Test
    public void plain() {
        ansi().out(helloWorld);
        Assert.assertEquals(ansiForTests.getStdout(), helloWorld);
    }

    @Test
    public void plainln() {
        ansi().outln(helloWorld);
        Assert.assertEquals(ansiForTests.getStdout(), helloWorld + "\n");
    }
}
