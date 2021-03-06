package com.mwdiamond.fansi;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Mechanism for writing ANSI codes to strings for testing, rather than stdout and stderr. Use this
 * class to write unit and functional tests involving {@link Ansi}. Most users should not need this
 * class, unless you're testing calls to System.out / System.err. You might want to use
 * {@link AnsiFactory#NO_OP} instead if you care about your program's output, not this library's.
 *
 * <p>Note that the exact outputs of {@code Ansi} is not guaranteed to be stable. Avoid writing
 * change-detector style tests that look for exact string matches, as these could easily break in
 * future versions. Consider instead looking for patterns, such as {@code .*Foobar.*\n}.
 */
public class AnsiForTests implements AnsiFactory {
  private final ByteArrayOutputStream stdoutSink = new ByteArrayOutputStream();
  private final ByteArrayOutputStream stderrSink = new ByteArrayOutputStream();
  private final PrintStream stdout = new PrintStream(stdoutSink);
  private final PrintStream stderr = new PrintStream(stderrSink);
  private final Codes codes;
  private final SystemInfo systemInfo;

  public AnsiForTests() {
    this(Codes.RAW, new SystemInfoForTests());
  }

  AnsiForTests(Codes codes, SystemInfo systemInfo) {
    this.codes = checkNotNull(codes);
    this.systemInfo = checkNotNull(systemInfo);
  }

  /**
   * Construct an {@code Ansi} instance associated with the stdout and stderr used by this
   * {@code AnsiForTests} instance.
   *
   * @return a mock Ansi instance.
   */
  @Override
  public Ansi ansi() {
    return new Ansi(stdout, stderr, codes, systemInfo);
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
   * Clears any output written thusfar to stdout.
   */
  public void clearStdout() {
    stdoutSink.reset();
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

  /**
   * Clears any output written thusfar to stderr.
   */
  public void clearStderr() {
    stderrSink.reset();
  }

  /**
   * Fake implementation of {@code SystemInfo} that does not access the system.
   */
  static class SystemInfoForTests extends SystemInfo {
    private final Codes codes;
    private final Integer columns;

    /** No system info available, always falls back to the defaults. */
    SystemInfoForTests() {
      this(null, null);
    }

    /** Uses the given (possibly null) values as the information retrieved from the system. */
    SystemInfoForTests(Codes codes, Integer columns) {
      this.codes = codes;
      this.columns = columns;
    }

    @Override
    Codes systemCodes() {
      return codes;
    }

    @Override
    Integer systemColumns() {
      return columns;
    }
  }
}
