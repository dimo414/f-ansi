package com.mwdiamond.fansi;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class AnsiForTests {
    private final ByteArrayOutputStream stdoutSink = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stderrSink = new ByteArrayOutputStream();
    private final PrintStream stdout = new PrintStream(stdoutSink);
    private final PrintStream stderr = new PrintStream(stderrSink);
    private final Ansi ansi;

    public AnsiForTests() {
        this(Codes.REAL);
    }

    public AnsiForTests(Codes codes) {
        ansi = new Ansi(stdout, stderr, codes);
    }

    public Ansi ansi() {
        return ansi;
    }

    public String getStdout() {
        // It's OK that this uses the default charset, since the stdout PrintStream also uses it
        // and nothing else can write to stdoutSink directly.
        return stdoutSink.toString();
    }

    public String getStderr() {
        // It's OK that this uses the default charset, since the stderr PrintStream also uses it
        // and nothing else can write to stderrSink directly.
        return stderrSink.toString();
    }
}
