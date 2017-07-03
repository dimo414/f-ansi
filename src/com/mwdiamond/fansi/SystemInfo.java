package com.mwdiamond.fansi;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Compartmentalizes work done to inspect the OS and environment being used.
 */
// TODO this class can only realistically be tested via system-aware integration tests
abstract class SystemInfo {
  private static final String PROPERTY_PREFIX = "com.mwdiamond.fansi.";
  private static final String ANSI_PROPERTY = PROPERTY_PREFIX + "ansi";
  private static final String DEBUG_PROPERTY = PROPERTY_PREFIX + "debug";

  private static final SystemInfo INSTANCE = new SystemInfo() {
    @Override
    Codes systemCodes() {
      return systemCodes;
    }

    @Override
    Integer systemColumns() {
        return systemColumns;
    }
  };

  static SystemInfo get() {
    return INSTANCE;
  }

  /**
   * Returns the {@code Codes} instance to be used by {@link Ansi} instances in this application.
   */
  final Codes codes(Codes defaultCodes) {
    Codes fromSystem = systemCodes();
    if (fromSystem != null) {
      return fromSystem;
    }
    return defaultCodes;
  }

  abstract Codes systemCodes();

  /**
   * Returns the known number of columns of the running terminal. This result may change over time,
   * so it should not be assumed to be constant or cached anywhere.
   */
  final int columns(int defaultColumns) {
    Integer fromSystem = systemColumns();
    if (fromSystem != null) {
      return fromSystem;
    }
    return defaultColumns;
  }

  abstract Integer systemColumns();

  private static final ThreadFactory threadFactory = new ThreadFactoryBuilder()
      .setNameFormat("F-ANSI-%d")
      .setPriority(Thread.MIN_PRIORITY)
      .setDaemon(true)
      .build();

  //
  // Static values set when this class is first loaded
  //

  private static final boolean debugOutput = getDebugStatusFromProperty();
  private static final Codes systemCodes = getCodesFromProperty();

  /*
   * This <i>may</i> be lazily updated with a column count from the system - see static {} block
   * at bottom.
   */
  private static volatile Integer systemColumns = getColumnsFromEnvironment();

  //
  // Static utilities
  //

  private static boolean getDebugStatusFromProperty() {
    String debugProperty = System.getProperty(DEBUG_PROPERTY, "false").toLowerCase();
    checkState(debugProperty.equals("true") || debugProperty.equals("false"),
        "Invalid value for %s, expected true/false, was %s",
        DEBUG_PROPERTY, debugProperty);
    return debugProperty.equals("true");
  }

  private static Codes getCodesFromProperty() {
    String codesProperty = System.getProperty(ANSI_PROPERTY);
    if (codesProperty == null) {
      return null;
    }
    switch (codesProperty.toUpperCase()) {
      case "REAL":
        return Codes.REAL;
      case "RAW":
        return Codes.RAW;
      case "OFF":
        return Codes.NO_OP;
      case "CONSOLE":
        return System.console() != null ? Codes.REAL : Codes.NO_OP;
      default:
        throw new IllegalStateException(
            "Invalid value " + codesProperty + " for property " + ANSI_PROPERTY);
    }
  }

  private static Integer getColumnsFromEnvironment() {
    return Ints.tryParse(Strings.nullToEmpty(System.getenv("COLUMNS")));
  }

  /**
   * Attempts to determine the number of columns in the terminal by making system calls, updating
   * the {@link #systemColumns} field if successful.
   *
   * <p>See https://stackoverflow.com/a/18883172/113632 for more.
   */
  private static void lookupColumnsFromSystem() {
    if (systemColumns != null) {
      return;
    }

    Thread t = threadFactory.newThread(new Runnable() {
      @Override
      public void run() {
        try {
          // It's not sufficient to just let this fail on Windows; if WSL is installed the "bash"
          // command will actually succeed and report the width of *that* terminal, not this one.
          if (System.getProperty("os.name").startsWith("Windows")) {
            return;
          }

          ProcessBuilder pb = new ProcessBuilder().command("bash", "-c", "tput cols 2> /dev/tty")
              .redirectOutput(ProcessBuilder.Redirect.PIPE)
              .redirectError(ProcessBuilder.Redirect.PIPE);
          Process p = pb.start();
          if (p.waitFor() != 0) {
            if (debugOutput) {
              System.err.println(pb.command() + " failed with exit code " + p.exitValue());
              System.err.println("STDOUT: " + inputStreamToString(p.getInputStream()));
              System.err.println("STDERR: " + inputStreamToString(p.getErrorStream()));
            }
            return; // no need to attempt to read if the command failed
          }
          String output = inputStreamToString(p.getInputStream());
          systemColumns = Integer.parseInt(output.trim());
        } catch (IOException | RuntimeException e) {
          // nothing to do, will fall-back to regular behavior
          if (debugOutput) {
            System.err.println("Unexpected exception while getting column count");
            e.printStackTrace(System.err);
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
    t.start();
  }

  private static String inputStreamToString(InputStream stream) throws IOException {
    return CharStreams.toString(new InputStreamReader(stream, StandardCharsets.UTF_8));
  }

  static {
    lookupColumnsFromSystem();
  }

  /** Diagnostic utility. */
  public static void main(String[] args) {
    System.out.println("Debug Mode?: " + debugOutput);
    System.out.println("System Codes: " + systemCodes);
    System.out.println("System Columns: " + systemColumns);
    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
    System.out.println("Delayed System Columns: " + systemColumns);
  }
}
