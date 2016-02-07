package com.mwdiamond.fansi;

import static com.google.common.base.Preconditions.checkState;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * Entry point to the F-ANSI library. Generally you should use the ansi()
 * method to obtain an instance of this class, so the library can determine
 * the appropriate syntax to use for the environment.
 *
 * Ansi instances are stateful, and are not intended to be or assigned to
 * variables. Instead, chain off the ansi() method directly to compose the
 * behavior you need.
 *
 * Note that the "terminating" output methods (out, outln, err, errln) return
 * an Ansi instance so you can continue chaining, but this is simply a
 * convenience; no state from before the terminating method carries over.
 *
 * For example:
 *
 * <code>ansi().color(RED).out("Hello ").background(GREEN).out("World");</code>
 *
 * does not render "World" in red text, only a green background.
 */
public class Ansi {
    // Might make this dynamic in the future, based on the startup environment
    private static final Codes DEFAULT_CODES = Codes.REAL;

    /**
     * An instance of Ansi configured for the current environment. Generally
     * you should use this method to obtain an Ansi instance.
     *
     * <p>Consider importing this method statically:
     * <code>import static com.mwdiamond.fansi.Ansi.ansi;</code>
     */
    public static Ansi ansi() {
        return new Ansi(DEFAULT_CODES);
    }

    /**
     * An instance of Ansi that always formats strings according to the Ansi
     * standard, even when other environments were detected.
     */
    public static Ansi realAnsi() {
        return new Ansi(Codes.REAL);
    }

    /**
     * An instance of Ansi that formats strings with the character sequences
     * used to create escape codes. Useful for copying or piping into Bash.
     */
    public static Ansi rawAnsi() {
        return new Ansi(Codes.RAW);
    }

    /**
     * Colors defined by the ANSI standard.
     *
     * <p>Consider importing this enum statically:
     * <code>import static com.mwdiamond.fansi.Ansi.Color.*;</code>
     */
    public static enum Color {
        BLACK(30),    DARK_GREY(90),
        RED(31),      LIGHT_RED(91),
        GREEN(32),    LIGHT_GREEN(92),
        YELLOW(33),   LIGHT_YELLOW(93),
        BLUE(34),     LIGHT_BLUE(94),
        MAGENTA(35),  LIGHT_MAGENTA(95),
        PURPLE(35),   LIGHT_PURPLE(95),
        CYAN(36),     LIGHT_CYAN(96),
        GREY(37),     WHITE(97),
        DEFAULT(39);

        private static final int BACKGROUND = 10; // additive
        private static final int EXTENDED = 38;
        private static final int RGB = 2;
        private static final int COLOR_INDEX = 5;

        private final int code;

        private Color(int code) {
            this.code = code;
        }

        int color() {
            return code;
        }

        int background() {
            return color() + BACKGROUND;
        }

        static List<Object> extended(int colorIndex, boolean background) {
            int code = background ? EXTENDED + BACKGROUND : EXTENDED;
            return ImmutableList.<Object>of(code, COLOR_INDEX, colorIndex);
        }

        static List<Object> extended(java.awt.Color color, boolean background) {
            int code = background ? EXTENDED + BACKGROUND : EXTENDED;
            return ImmutableList.<Object>of(code, RGB, color.getRed(), color.getGreen(), color.getBlue());
        }
    }

    static class ColorType {
        static ColorType DEFAULT = new ColorType(Color.DEFAULT);

        private Color namedColor;
        private Integer n8BitColor;
        private java.awt.Color n24BitColor;

        ColorType(Color color) {
            namedColor = color;
        }

        ColorType(int color) {
            n8BitColor = color;
        }

        ColorType(java.awt.Color color) {
            n24BitColor = color;
        }

        Color namedColor() {
            return namedColor;
        }

        Integer n8BitColor() {
            return n8BitColor;
        }

        java.awt.Color n24BitColor() {
            return n24BitColor;
        }
    }

    /**
     * Fonts defined by the ANSI standard.
     *
     * <p>Consider importing this enum statically:
     * <code>import static com.mwdiamond.fansi.Ansi.Font.*;</code>
     */
    public static enum Font {
        F1(11), F2(12), F3(13), F4(14), F5(15), F6(16), F7(17), F8(18), F9(19), FRAKTUR(20), DEFAULT(10);

        private final int code;

        private Font(int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }
    }

    /**
     * Styles defined by the ANSI standard.
     *
     * <p>Consider importing this enum statically:
     * <code>import static com.mwdiamond.fansi.Ansi.Style.*;</code>
     */
    public static enum Style {
        BOLD(1), DIM(2), ITALIC(3), UNDERLINE(4), BLINK(5), BLINK_RAPID(6), REVERSE(7), CONCEAL(8), STRIKETHROUGH(9),
        FRAME(51), ENCIRCLE(52), OVERLINE(53);

        private final int code;

        private Style(int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }
    }

    private final PrintStream stdout;
    private final PrintStream stderr;
    private final Codes codes;
    private final LinkedList<String> preBuffer;
    private final LinkedList<String> postBuffer;

    private Ansi(Codes codes) {
        this(System.out, System.err, codes);
    }

    // Package-visible for AnsiForTests
    Ansi(PrintStream stdout, PrintStream stderr, Codes codes) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.codes = codes;
        preBuffer = new LinkedList<>();
        postBuffer = new LinkedList<>();
    }

    private void prepend(String ... parts) {
        for (String part : parts) {
            preBuffer.add(part);
        }
    }

    private void append(String ... parts) {
        // prepend in reverse order, so the list ends up in the same order
        for (int i = parts.length - 1; i >= 0; i--) {
            postBuffer.addFirst(parts[i]);
        }
    }

    /** Sets the title of the current window. */
    public void title(String title) {
        checkState(preBuffer.isEmpty() && postBuffer.isEmpty(), "Unnecessary chaining; cannot set additional formatting on the window title.");
        out(codes.title(title));
    }

    /**
     * Sets the color, and optionally the style(s), of the next block of text
     * to display.
     */
    public Ansi color(Color color, Style ... styles) {
        prepend(codes.color(new ColorType(color), ColorType.DEFAULT, Font.DEFAULT, styles));
        append(codes.clear());
        return this;
    }

    /**
     * Sets the color, and optionally the style(s), of the next block of text
     * to display.
     *
     * TODO add link to 8-bit color reference.
     */
    public Ansi color(int color8Bit, Style ... styles) {
        prepend(codes.color(new ColorType(color8Bit), ColorType.DEFAULT, Font.DEFAULT, styles));
        append(codes.clear());
        return this;
    }

    /**
     * Sets the color, and optionally the style(s), of the next block of text
     * to display.
     *
     * <p>Note that 24-bit color support is not universal. You should prefer to use Ansi.Color
     * values where possible.
     */
    public Ansi color(java.awt.Color color, Style ... styles) {
        prepend(codes.color(new ColorType(color), ColorType.DEFAULT, Font.DEFAULT, styles));
        append(codes.clear());
        return this;
    }

    /**
     * Sets the color, font, and optionally the style(s), of the next block of
     * text to display.
     */
    public Ansi color(Color color, Font font, Style ... styles) {
        prepend(codes.color(new ColorType(color), ColorType.DEFAULT, font, styles));
        append(codes.clearFont(), codes.clear());
        return this;
    }

    /**
     * Sets the color, font, and optionally the style(s), of the next block of
     * text to display.
     *
     * TODO add link to 8-bit color reference.
     */
    public Ansi color(int color8Bit, Font font, Style ... styles) {
        prepend(codes.color(new ColorType(color8Bit), ColorType.DEFAULT, font, styles));
        append(codes.clearFont(), codes.clear());
        return this;
    }

    /**
     * Sets the color, font, and optionally the style(s), of the next block of
     * text to display.
     *
     * <p>Note that 24-bit color support is not universal. You should prefer to use Ansi.Color
     * values where possible.
     */
    public Ansi color(java.awt.Color color, Font font, Style ... styles) {
        prepend(codes.color(new ColorType(color), ColorType.DEFAULT, font, styles));
        append(codes.clearFont(), codes.clear());
        return this;
    }

    /**
     * Sets the color and background, and optionally the style(s), of the next
     * block of text to display.
     */
    public Ansi color(Color color, Color background, Style ... styles) {
        prepend(codes.color(new ColorType(color), new ColorType(background), Font.DEFAULT, styles));
        append(codes.clear());
        return this;
    }

    /**
     * Sets the color and background, and optionally the style(s), of the next
     * block of text to display.
     *
     * TODO add link to 8-bit color reference.
     */
    public Ansi color(int color8Bit, int background8Bit, Style ... styles) {
        prepend(codes.color(new ColorType(color8Bit), new ColorType(background8Bit), Font.DEFAULT, styles));
        append(codes.clear());
        return this;
    }

    /**
     * Sets the color and background, and optionally the style(s), of the next
     * block of text to display.
     *
     * <p>Note that 24-bit color support is not universal. You should prefer to use Ansi.Color
     * values where possible.
     */
    public Ansi color(java.awt.Color color, java.awt.Color background, Style ... styles) {
        prepend(codes.color(new ColorType(color), new ColorType(background), Font.DEFAULT, styles));
        append(codes.clear());
        return this;
    }

    /**
     * Sets the color, background, font, and optionally style(s), of the next
     * block of text to display.
     */
    public Ansi color(Color color, Color background, Font font, Style ... styles) {
        prepend(codes.color(new ColorType(color), new ColorType(background), font, styles));
        append(codes.clearFont(), codes.clear());
        return this;
    }

    /**
     * Sets the color, background, font, and optionally style(s), of the next
     * block of text to display.
     *
     * TODO add link to 8-bit color reference.
     */
    public Ansi color(int color8Bit, int background8Bit, Font font, Style ... styles) {
        prepend(codes.color(new ColorType(color8Bit), new ColorType(background8Bit), font, styles));
        append(codes.clearFont(), codes.clear());
        return this;
    }

    /**
     * Sets the color, background, font, and optionally style(s), of the next
     * block of text to display.
     *
     * <p>Note that 24-bit color support is not universal. You should prefer to use Ansi.Color
     * values where possible.
     */
    public Ansi color(java.awt.Color color, java.awt.Color background, Font font, Style ... styles) {
        prepend(codes.color(new ColorType(color), new ColorType(background), font, styles));
        append(codes.clearFont(), codes.clear());
        return this;
    }

    /**
     * Sets the background color, and optionally the style(s), of the next
     * block of text to display.
     */
    public Ansi background(Color background, Style ... styles) {
        prepend(codes.color(ColorType.DEFAULT, new ColorType(background), Font.DEFAULT, styles));
        append(codes.clear());
        return this;
    }

    /**
     * Sets the background color, and optionally the style(s), of the next
     * block of text to display.
     *
     * TODO add link to 8-bit color reference.
     */
    public Ansi background(int background8Bit, Style ... styles) {
        prepend(codes.color(ColorType.DEFAULT, new ColorType(background8Bit), Font.DEFAULT, styles));
        append(codes.clear());
        return this;
    }

    /**
     * Sets the background color, and optionally the style(s), of the next
     * block of text to display.
     *
     * <p>Note that 24-bit color support is not universal. You should prefer to use Ansi.Color
     * values where possible.
     */
    public Ansi background(java.awt.Color background, Style ... styles) {
        prepend(codes.color(ColorType.DEFAULT, new ColorType(background), Font.DEFAULT, styles));
        append(codes.clear());
        return this;
    }

    /**
     * Sets the font, and optionally the style(s), of the next block of text to
     * display.
     */
    public Ansi font(Font font, Style ... styles) {
        prepend(codes.color(ColorType.DEFAULT, ColorType.DEFAULT, font, styles));
        append(codes.clearFont(), codes.clear());
        return this;
    }

    /**
     * Sets the color, and optionally the style(s), of the next block of text to
     * display.
     */
    public Ansi style(Style style, Style ... styles) {
        Style[] merged = new Style[styles.length + 1];
        merged[0] = style;
        System.arraycopy(styles, 0, merged, 1, styles.length);
        prepend(codes.color(ColorType.DEFAULT, ColorType.DEFAULT, Font.DEFAULT, merged));
        append(codes.clear());
        return this;
    }

    /**
     * Repositions the cursor up or down a number of lines. Negative values
     * move the cursor up, positive values move it down. Saves the cursor
     * position so it can be restored with <code>restoreCursor()</code>.
     */
    public Ansi moveCursor(int lines) {
        prepend(codes.saveCursor());
        if (lines < 0) {
            prepend(codes.upLine(0 - lines));
        } else {
            prepend(codes.downLine(lines)); // will raise an exception if 0
        }
        return this;
    }

    /**
     * Move the cursor up or down a number of lines and left or right a number
     * of columns. Negative values are up/left, positive values are down/right.
     * Saves the cursor position so it can be restored with
     * <code>restoreCursor()</code>.
     */
    public Ansi moveCursor(int lines, int columns) {
        prepend(codes.saveCursor(), codes.moveCursor(lines, columns));
        return this;
    }

    /**
     * Moves the cursor to a fixed position on the screen, where the top left
     * corner is 1, 1. Restores the cursor to its original location afterwards.
     */
    public Ansi fixed(int row, int column) {
        prepend(codes.saveCursor(), codes.positionCursor(row, column));
        append(codes.restoreCursor());
        return this;
    }

    /**
     * Restores the cursor to its previously saved position; use in tandem with
     * <code>moveCursor</code>.
     */
    public Ansi restoreCursor() {
        prepend(codes.restoreCursor());
        return this;
    }

    /**
     * Clears the current line and positions the cursor at the start of the
     * line.
     */
    public Ansi overwriteThisLine() {
        prepend(codes.clearLine());
        return this;
    }

    /**
     * Clears the current and previous lines and positions the cursor at the
     * start of the previous line.
     */
    public Ansi overwriteLastLine() {
        prepend(codes.clearLine(), codes.upLine(1), codes.clearLine());
        return this;
    }

    private Ansi writeToPrintStream(PrintStream out, boolean newLine, String text, Object ... args) {
        StringBuilder buffer = new StringBuilder();
        for (String part : preBuffer) {
            buffer.append(part);
        }
        buffer.append(String.format(text, args));
        for (String part : postBuffer) {
            buffer.append(part);
        }

        if (newLine) {
            out.println(buffer);
        } else {
            out.print(buffer);
        }

        preBuffer.clear();
        postBuffer.clear();
        return this;
    }

    /**
     * Writes text to stdout after piping it and args through String.format().
     *
     * <p>Compare to System.out.print()
     */
    public Ansi out(String text, Object ... args) {
        return writeToPrintStream(stdout, false, text, args);
    }

    /**
     * Writes text to stdout after piping it and args through String.format(),
     * also prints a newline.
     *
     * Compare to System.out.println()
     */
    public Ansi outln(String text, Object ... args) {
        return writeToPrintStream(stdout, true, text, args);
    }

    /**
     * Simply writes a newline to stdout.
     *
     * Compare to System.out.println()
     */
    public Ansi outln() {
        return writeToPrintStream(stdout, true, "");
    }

    /**
     * Writes text to stderr after piping it and args through String.format().
     *
     * <p>Compare to System.err.print()
     */
    public Ansi err(String text, Object ... args) {
        return writeToPrintStream(stderr, false, text, args);
    }

    /**
     * Writes text to stderr after piping it and args through String.format(),
     * also prints a newline.
     *
     * Compare to System.err.println()
     */
    public Ansi errln(String text, Object ... args) {
        return writeToPrintStream(stderr, true, text, args);
    }

    /**
     * Simply writes a newline to stderr.
     *
     * Compare to System.err.println()
     */
    public Ansi errln() {
        return writeToPrintStream(stderr, true, "");
    }
}
