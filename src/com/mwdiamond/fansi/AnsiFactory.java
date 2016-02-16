package com.mwdiamond.fansi;

/**
 * A factory for providing Ansi instances in a dependency-injection friendly
 * manor. While most users will be happiest with the static ansi() method of
 * the Ansi class, more complex use cases may need to select different Ansi
 * instances depending on the environment (for instance in tests).
 */
public interface AnsiFactory {
    /**
     * A factory wrapper for <code>Ansi.ansi()</code>.
     */
    public static AnsiFactory DEFAULT = new AnsiFactory() {
        @Override
        public Ansi ansi() {
            return Ansi.ansi();
        }
    };

    /**
     * A factory that returns Ansi instances that output no escape codes,
     * and simply write output unchanged to stdout and stderr.
     */
    public static AnsiFactory NO_OP = new AnsiFactory() {
        @Override
        public Ansi ansi() {
            return Ansi.noOpAnsi();
        }
    };

    /**
     * @return An Ansi instance.
     */
    public Ansi ansi();
}
