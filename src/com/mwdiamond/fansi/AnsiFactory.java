package com.mwdiamond.fansi;

/**
 * A factory for providing {@code Ansi} instances in a dependency-injection friendly manor. While
 * most users will be happiest with the static {@link Ansi#ansi} method, more complex use cases may
 * need to select different {@code Ansi} instances depending on the environment (for instance in
 * tests).
 */
public interface AnsiFactory {
  /**
   * A factory wrapper for {@link Ansi#ansi}.
   */
  public static AnsiFactory DEFAULT = new AnsiFactory() {
    @Override
    public Ansi ansi() {
      return Ansi.ansi();
    }
  };

  /**
   * A factory wrapper for {@link Ansi#realAnsi}.
   */
  public static AnsiFactory REAL = new AnsiFactory() {
    @Override
    public Ansi ansi() {
      return Ansi.realAnsi();
    }
  };

  /**
   * A factory wrapper for {@link Ansi#rawAnsi}.
   */
  public static AnsiFactory RAW = new AnsiFactory() {
    @Override
    public Ansi ansi() {
      return Ansi.rawAnsi();
    }
  };

  /**
   * A factory that returns {@code Ansi} instances that output no escape codes, and simply write
   * output unchanged to stdout and stderr.
   */
  public static AnsiFactory NO_OP = new AnsiFactory() {
    @Override
    public Ansi ansi() {
      return Ansi.noOpAnsi();
    }
  };

  /**
   * Constructs a new {@code Ansi} instance as specified by this factory implementation.
   *
   * @return An Ansi instance.
   */
  public Ansi ansi();
}
