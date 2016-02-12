package com.mwdiamond.fansi;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Mechanism for writing ANSI codes to strings for testing, rather than
 * stdout and stderr. Use this class to write unit and functional tests
 * involving Ansi. Most users should not need this class, unless you're testing
 * calls to System.out / System.err.
 *
 * <p>Note that the exact outputs of Ansi are not guaranteed to be stable.
 * Avoid writing change-detector style tests that look for exact string
 * matches, as these could easily break in future versions. Consider
 * instead looking for patterns, such as <code>.*Foobar.*\n</code>.
 */
public class AnsiForTests {
    private final ByteArrayOutputStream stdoutSink = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stderrSink = new ByteArrayOutputStream();
    private final PrintStream stdout = new PrintStream(stdoutSink);
    private final PrintStream stderr = new PrintStream(stderrSink);
    private final Codes codes;

    public AnsiForTests() {
        this(Codes.REAL);
    }

    AnsiForTests(Codes codes) {
        this.codes = checkNotNull(codes);
    }

    /**
     * Get an Ansi instance associated with the stdout and stderr used by this
     * AnsiForTests instance.
     *
     * @return a mock Ansi instance.
     */
    public Ansi ansi() {
        return new Ansi(stdout, stderr, codes);
    }

    /**
     * The output that's been written to stdout.
     *
     * @return The contents of this instance's stdout.
     */
    public String getStdout() {
        // It's OK that this uses the default charset, since the stdout PrintStream also uses it
        // and nothing else can write to stdoutSink directly.
        return stdoutSink.toString();
    }

    /**
     * The output that's been written to stderr.
     *
     * @return The contents of this instance's stderr.
     */
    public String getStderr() {
        // It's OK that this uses the default charset, since the stderr PrintStream also uses it
        // and nothing else can write to stderrSink directly.
        return stderrSink.toString();
    }
}
