package com.mwdiamond.fansi;

import static com.google.common.truth.Truth.assertThat;
import static com.mwdiamond.fansi.Ansi.Color.*;
import static com.mwdiamond.fansi.Ansi.Font.*;
import static com.mwdiamond.fansi.Ansi.Style.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Basic unit tests of Ansi, including (sadly) a number of change
 * detector-style tests that may be brittle.
 */
public class AnsiTest {
    private static final String helloWorld = "Hello World";
    private AnsiForTests ansiForTests;

    /**
     * Annotation indicating a test method is a change-detector, verifying
     * behavior that may not be part of the public API, and therefore could
     * change. When these methods need to be updated increment the timesUpdated
     * field as a record of how often this occurs. A high update count is a
     * good indication that a test should be refactored or removed.
     */
    @Target({ElementType.METHOD})
    private static @interface ChangeDetector {
        int timesUpdated();
    }

    @BeforeMethod
    private void flushAnsi() {
        ansiForTests = new AnsiForTests(Codes.RAW);
    }

    private Ansi ansi() {
        return ansiForTests.ansi();
    }

    @Test
    public void plain() {
        ansi().out(helloWorld);
        assertThat(ansiForTests.getStdout()).isEqualTo(helloWorld);
        assertThat(ansiForTests.getStderr()).isEmpty();
    }

    @Test
    public void plainLn() {
        ansi().outln(helloWorld);
        assertThat(ansiForTests.getStdout()).isEqualTo(helloWorld + "\n");
        assertThat(ansiForTests.getStderr()).isEmpty();
    }

    @Test
    public void plainErr() {
        ansi().err(helloWorld);
        assertThat(ansiForTests.getStdout()).isEmpty();
        assertThat(ansiForTests.getStderr()).isEqualTo(helloWorld);
    }

    @Test
    public void plainErrLn() {
        ansi().errln(helloWorld);
        assertThat(ansiForTests.getStdout()).isEmpty();
        assertThat(ansiForTests.getStderr()).isEqualTo(helloWorld + "\n");
    }

    @Test
    @ChangeDetector(timesUpdated = 0)
    public void setTitle() {
        ansi().title(helloWorld);
        assertThat(ansiForTests.getStdout()).isEqualTo("\\e]0;" + helloWorld + "\\a");
    }

    @Test
    @ChangeDetector(timesUpdated = 0)
    public void color() {
        ansi().color(RED).out(helloWorld);
        assertThat(ansiForTests.getStdout()).isEqualTo("\\e[31m" + helloWorld + "\\e[m");
    }

    @Test
    @ChangeDetector(timesUpdated = 0)
    public void colorBackground() {
        ansi().color(GREEN, RED).out(helloWorld);
        assertThat(ansiForTests.getStdout()).isEqualTo("\\e[32;41m" + helloWorld + "\\e[m");
    }

    @Test
    @ChangeDetector(timesUpdated = 0)
    public void colorStyle() {
        ansi().color(YELLOW, BOLD).out(helloWorld);
        assertThat(ansiForTests.getStdout()).isEqualTo("\\e[1;33m" + helloWorld + "\\e[m");
    }

    @Test
    @ChangeDetector(timesUpdated = 0)
    public void colorStyles() {
        ansi().color(CYAN, BOLD, ITALIC).out(helloWorld);
        assertThat(ansiForTests.getStdout()).isEqualTo("\\e[1;3;36m" + helloWorld + "\\e[m");
    }

    @Test
    @ChangeDetector(timesUpdated = 0)
    public void colorFont() {
        ansi().color(BLUE, F1).out(helloWorld);
        assertThat(ansiForTests.getStdout()).isEqualTo("\\e[11;34m" + helloWorld + "\\e[10m\\e[m");
    }

    @Test
    public void chainedColor() {
        ansi().color(BLUE).out(helloWorld).color(RED, GREEN).out(helloWorld);
        assertThat(ansiForTests.getStdout()).isEqualTo("\\e[34m" + helloWorld + "\\e[m\\e[31;42m" + helloWorld + "\\e[m");
    }

    @Test
    @ChangeDetector(timesUpdated = 1)
    public void color8Bit() {
        ansi().color(100, 200).out(helloWorld);
        assertThat(ansiForTests.getStdout()).isEqualTo("\\e[38;5;100;48;5;200m" + helloWorld + "\\e[m");
    }

    @Test
    @ChangeDetector(timesUpdated = 1)
    public void color24Bit() {
        ansi().color(java.awt.Color.RED, java.awt.Color.BLUE).out(helloWorld);
        assertThat(ansiForTests.getStdout()).isEqualTo("\\e[38;2;255;0;0;48;2;0;0;255m" + helloWorld + "\\e[m");
    }

    @Test
    @ChangeDetector(timesUpdated = 0)
    public void fixed() {
        ansi().fixed(10, 20).out(helloWorld);
        assertThat(ansiForTests.getStdout()).isEqualTo("\\e[s\\e[10;20H" + helloWorld + "\\e[u");
    }

    @Test
    public void fixedColor() {
        ansi().fixed(10, 20).color(RED).outln(helloWorld);
        ansi().color(RED).fixed(10, 20).outln(helloWorld);
        assertThat(ansiForTests.getStdout()).isEqualTo(
            "\\e[s\\e[10;20H\\e[31m" + helloWorld + "\\e[m\\e[u\n" +
            "\\e[31m\\e[s\\e[10;20H" + helloWorld + "\\e[u\\e[m\n"
        );
    }

    @Test
    @ChangeDetector(timesUpdated = 0)
    public void overwriteThisLine() {
        ansi().overwriteThisLine().out(helloWorld);
        assertThat(ansiForTests.getStdout()).isEqualTo("\\e[2K" + helloWorld);
    }

    @Test
    @ChangeDetector(timesUpdated = 0)
    public void overwriteLastLine() {
        ansi().overwriteLastLine().out(helloWorld);
        assertThat(ansiForTests.getStdout()).isEqualTo("\\e[2K\\e[1F\\e[2K" + helloWorld);
    }
}
