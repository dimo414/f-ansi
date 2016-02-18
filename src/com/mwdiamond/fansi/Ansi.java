package com.mwdiamond.fansi;

import static com.google.common.base.Preconditions.checkState;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import com.mwdiamond.fansi.Codes.ColorType;

/**
 * The Ansi class wraps an application's stdout and stderr to provide
 * additional console functionality via
 * <a href="https://en.wikipedia.org/wiki/ANSI_escape_code">ANSI escape codes</a>,
 * such as colored output and cursor repositioning. Generally you should use
 * the <code>ansi()</code> method to obtain an instance of this class, which
 * will dynamically determine the appropriate syntax to use for the current
 * environment.
 *
 * <p>The ANSI standard is not universally implemented, and behavior will be
 * terminal-specific. For example while most terminals support the standard
 * named colors, support for the more versatile color indexes and 24-bit
 * full-color is less consistent. Cursor movement support is similarly
 * limited on certain terminals. Be sure to test the behavior you expect
 * on any terminals you intend to support.
 *
 * <p>Ansi instances are stateful, and are not intended to be persisted,
 * assigned to variables, or used across threads. Instead, chain off the
 * <code>ansi()</code> method directly to compose the behavior you need in a
 * fluent style.
 *
 * <p>Note that the "terminating" output methods (out, outln, err, errln) return
 * an Ansi instance so you can continue chaining, but this is simply a
 * convenience; no state from before the terminating method carries over.
 *
 * <p>For example:
 *
 * <pre><code>ansi().color(RED).out("Hello ").background(GREEN).out("World");</code></pre>
 *
 * <p>does not render "World" in red text, only a green background.
 */
public class Ansi {
    private static final long DEFAULT_DELAY = 100;
    // Might make this dynamic in the future, based on the startup environment
    // E.g. check if System.console() returns null
    private static final Codes DEFAULT_CODES = Codes.REAL;

    /**
     * An instance of Ansi configured for the current environment. Generally
     * you should use this method to obtain an Ansi instance.
     *
     * <p>Consider importing this method statically:
     * <code>import static com.mwdiamond.fansi.Ansi.ansi;</code>
     *
     * @return an Ansi instance wrapping System.out and System.err.
     */
    public static Ansi ansi() {
        // TODO read a Java property to determine the type of Ansi instance to return.
        // Property real: real
        // Property raw: raw
        // Property none: none
        // Property console: real if console, none if null
        return new Ansi(DEFAULT_CODES);
    }

    /**
     * An instance of Ansi that always formats strings according to the ANSI
     * standard, even when other environments were detected.
     *
     * @return an Ansi instance wrapping System.out and System.err.
     */
    public static Ansi realAnsi() {
        return new Ansi(Codes.REAL);
    }

    /**
     * An instance of Ansi that formats strings with the character sequences
     * used to create escape codes. Useful for copying or piping into Bash's
     * <code>echo -e</code>.
     *
     * @return an Ansi instance wrapping System.out and System.err.
     */
    public static Ansi rawAnsi() {
        return new Ansi(Codes.RAW);
    }

    /**
     * An instance of Ansi that doesn't output any escape codes.
     * No need to be public, there's little reason for a caller to manually ask
     * for a no-op Ansi; they could just print directly to stdout or stderr.
     *
     * @return an Ansi instance wrapping System.out and System.err.
     */
    static Ansi noOpAnsi() {
        return new Ansi(Codes.NO_OP);
    }

    /**
     * Colors defined by the ANSI standard.
     *
     * <p>These are likely to be supported by most modern terminals, but
     * different terminals may use different RGB values to render them.
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

        /** Returns the code associated with this color. */
        int color() {
            return code;
        }

        /** Returns the code associated with this color as a background. */
        int background() {
            return color() + BACKGROUND;
        }

        /** Creates a list of the codes to specify a color index. */
        static List<Object> extended(int colorIndex, boolean background) {
            int code = background ? EXTENDED + BACKGROUND : EXTENDED;
            return ImmutableList.<Object>of(code, COLOR_INDEX, colorIndex);
        }

        /** Creates a list of the codes to specify an RGB color. */
        static List<Object> extended(java.awt.Color color, boolean background) {
            int code = background ? EXTENDED + BACKGROUND : EXTENDED;
            return ImmutableList.<Object>of(code, RGB, color.getRed(), color.getGreen(), color.getBlue());
        }
    }

    /**
     * Maps a Java Color to a reasonably close color index, for terminals that
     * don't support full colors.
     *
     * @param color A Java color.
     * @return an approximately equivalent color index.
     */
    // http://stackoverflow.com/a/27165165/113632
    public static int toColorIndex(java.awt.Color color) {
        // 0x00-0x0F:  Native colors, unspecified and not provided here

        // 0x10-0xE7:  6 × 6 × 6 = 216 colors: 16 + 36 × r + 6 × g + b (0 ≤ r, g, b ≤ 5)
        int[] indicies = {toCode(color.getRed()), toCode(color.getGreen()), toCode(color.getBlue())};
        if (ImmutableSet.copyOf(Ints.asList(indicies)).size() > 1) {
            // If all indicies map to the same value, we'd return one of six
            // greys in the color section. We can do better by using the
            // greyscale section.
            return 0x10 + 0x24 * indicies[0] + 0x06 * indicies[1] + indicies[2];
        }

        // 0xE8-0xFF:  grayscale from black to white in 24 steps
        // Averaging probably isn't ideal for colors, but it's good enough;
        // R, G, and B should be fairly close to each other.
        int average = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
        int greycode = (average - 0x08) / 0x0A;
        if (greycode <= 0) {
            return 0x10; // black
        } else if (greycode > 23) {
            return 0xE7; // white
        }
        return 0xE8 + greycode;
    }

    private static int toCode(int component) {
        return Math.max(0, (component - 55) / 40);
    }

    /**
     * Fonts defined by the ANSI standard.
     *
     * <p>Many terminals do not support different fonts; it's likely specifying
     * these will do nothing.
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

        int code() {
            return code;
        }
    }

    /**
     * Styles defined by the ANSI standard.
     *
     * <p>Many terminals support at least a subset of these styles (notably
     * <code>BOLD</code>), but others are less common. Some, notably
     * <code>BLINK</code>, are intentionally disabled in many terminals.
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

        int code() {
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
        preBuffer.addAll(Arrays.asList(parts));
    }

    private void append(String ... parts) {
        // prepend in reverse order, so the list ends up in the same order
        for (int i = parts.length - 1; i >= 0; i--) {
            postBuffer.addFirst(parts[i]);
        }
    }

    /**
     * Sets the title of the current window.
     *
     * @param title text to make the current window title.
     * */
    public void title(String title) {
        checkState(preBuffer.isEmpty() && postBuffer.isEmpty(), "Unnecessary chaining; cannot set additional formatting on the window title.");
        out(codes.title(title));
    }

    /**
     * Sets the color, and optionally the style(s), of the next block of text
     * to display.
     *
     * @param color A standard ANSI color.
     * @param styles Any additional styles to apply, <em>optional</em>.
     * @return This Ansi instance, to continue modifying the output.
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
     * @param colorIndex A color index, 0-255.
     * @param styles Any additional ANSI styles to apply, <em>optional</em>.
     * @return This Ansi instance, to continue modifying the output.
     */
    // TODO add link to 8-bit color reference.
    public Ansi color(int colorIndex, Style ... styles) {
        prepend(codes.color(new ColorType(colorIndex), ColorType.DEFAULT, Font.DEFAULT, styles));
        append(codes.clear());
        return this;
    }

    /**
     * Sets the color, and optionally the style(s), of the next block of text
     * to display.
     *
     * <p>Note that 24-bit color support is not universal. You should prefer to use Ansi.Color
     * values where possible.
     *
     * @param color A Java color, which will be mapped to RGB integer values.
     * @param styles Any additional styles to apply, <em>optional</em>.
     * @return This Ansi instance, to continue modifying the output.
     */
    public Ansi color(java.awt.Color color, Style ... styles) {
        prepend(codes.color(new ColorType(color), ColorType.DEFAULT, Font.DEFAULT, styles));
        append(codes.clear());
        return this;
    }

    /**
     * Sets the color, font, and optionally the style(s), of the next block of
     * text to display.
     *
     * @param color A standard ANSI color.
     * @param font a standard ANSI font.
     * @param styles Any additional styles to apply, <em>optional</em>.
     * @return This Ansi instance, to continue modifying the output.
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
     * @param colorIndex A color index, 0-255.
     * @param font a standard ANSI font.
     * @param styles Any additional ANSI styles to apply, <em>optional</em>.
     * @return This Ansi instance, to continue modifying the output.
     */
    // TODO add link to 8-bit color reference.
    public Ansi color(int colorIndex, Font font, Style ... styles) {
        prepend(codes.color(new ColorType(colorIndex), ColorType.DEFAULT, font, styles));
        append(codes.clearFont(), codes.clear());
        return this;
    }

    /**
     * Sets the color, font, and optionally the style(s), of the next block of
     * text to display.
     *
     * <p>Note that 24-bit color support is not universal. You should prefer to use Ansi.Color
     * values where possible.
     *
     * @param color A Java color, which will be mapped to RGB integer values.
     * @param font a standard ANSI font.
     * @param styles Any additional styles to apply, <em>optional</em>.
     * @return This Ansi instance, to continue modifying the output.
     */
    public Ansi color(java.awt.Color color, Font font, Style ... styles) {
        prepend(codes.color(new ColorType(color), ColorType.DEFAULT, font, styles));
        append(codes.clearFont(), codes.clear());
        return this;
    }

    /**
     * Sets the color and background, and optionally the style(s), of the next
     * block of text to display.
     *
     * @param color A standard ANSI color.
     * @param background A standard ANSI color.
     * @param styles Any additional styles to apply, <em>optional</em>.
     * @return This Ansi instance, to continue modifying the output.
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
     * @param colorIndex A color index, 0-255.
     * @param backgroundIndex A color index, 0-255.
     * @param styles Any additional ANSI styles to apply, <em>optional</em>.
     * @return This Ansi instance, to continue modifying the output.
     */
    // TODO add link to 8-bit color reference.
    public Ansi color(int colorIndex, int backgroundIndex, Style ... styles) {
        prepend(codes.color(new ColorType(colorIndex), new ColorType(backgroundIndex), Font.DEFAULT, styles));
        append(codes.clear());
        return this;
    }

    /**
     * Sets the color and background, and optionally the style(s), of the next
     * block of text to display.
     *
     * <p>Note that 24-bit color support is not universal. You should prefer to use Ansi.Color
     * values where possible.
     *
     * @param color A Java color, which will be mapped to RGB integer values.
     * @param background A Java color, which will be mapped to RGB integer values.
     * @param styles Any additional styles to apply, <em>optional</em>.
     * @return This Ansi instance, to continue modifying the output.
     */
    public Ansi color(java.awt.Color color, java.awt.Color background, Style ... styles) {
        prepend(codes.color(new ColorType(color), new ColorType(background), Font.DEFAULT, styles));
        append(codes.clear());
        return this;
    }

    /**
     * Sets the color, background, font, and optionally style(s), of the next
     * block of text to display.
     *
     * @param color A standard ANSI color.
     * @param background A standard ANSI color.
     * @param font a standard ANSI font.
     * @param styles Any additional styles to apply, <em>optional</em>.
     * @return This Ansi instance, to continue modifying the output.
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
     * @param colorIndex A color index, 0-255.
     * @param backgroundIndex A color index, 0-255.
     * @param font a standard ANSI font.
     * @param styles Any additional ANSI styles to apply, <em>optional</em>.
     * @return This Ansi instance, to continue modifying the output.
     */
    // TODO add link to 8-bit color reference.
    public Ansi color(int colorIndex, int backgroundIndex, Font font, Style ... styles) {
        prepend(codes.color(new ColorType(colorIndex), new ColorType(backgroundIndex), font, styles));
        append(codes.clearFont(), codes.clear());
        return this;
    }

    /**
     * Sets the color, background, font, and optionally style(s), of the next
     * block of text to display.
     *
     * <p>Note that 24-bit color support is not universal. You should prefer to use Ansi.Color
     * values where possible.
     *
     * @param color A Java color, which will be mapped to RGB integer values.
     * @param background A Java color, which will be mapped to RGB integer values.
     * @param font a standard ANSI font.
     * @param styles Any additional styles to apply, <em>optional</em>.
     * @return This Ansi instance, to continue modifying the output.
     */
    public Ansi color(java.awt.Color color, java.awt.Color background, Font font, Style ... styles) {
        prepend(codes.color(new ColorType(color), new ColorType(background), font, styles));
        append(codes.clearFont(), codes.clear());
        return this;
    }

    /**
     * Sets the background color, and optionally the style(s), of the next
     * block of text to display.
     *
     * @param background A standard ANSI color.
     * @param styles Any additional styles to apply, <em>optional</em>.
     * @return This Ansi instance, to continue modifying the output.
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
     * @param backgroundIndex A color index, 0-255.
     * @param styles Any additional ANSI styles to apply, <em>optional</em>.
     * @return This Ansi instance, to continue modifying the output.
     */
    // TODO add link to 8-bit color reference.
    public Ansi background(int backgroundIndex, Style ... styles) {
        prepend(codes.color(ColorType.DEFAULT, new ColorType(backgroundIndex), Font.DEFAULT, styles));
        append(codes.clear());
        return this;
    }

    /**
     * Sets the background color, and optionally the style(s), of the next
     * block of text to display.
     *
     * <p>Note that 24-bit color support is not universal. You should prefer to use Ansi.Color
     * values where possible.
     *
     * @param background A Java color, which will be mapped to RGB integer values.
     * @param styles Any additional styles to apply, <em>optional</em>.
     * @return This Ansi instance, to continue modifying the output.
     */
    public Ansi background(java.awt.Color background, Style ... styles) {
        prepend(codes.color(ColorType.DEFAULT, new ColorType(background), Font.DEFAULT, styles));
        append(codes.clear());
        return this;
    }

    /**
     * Sets the font, and optionally the style(s), of the next block of text to
     * display.
     *
     * @param font a standard ANSI font.
     * @param styles Any additional ANSI styles to apply, <em>optional</em>.
     * @return This Ansi instance, to continue modifying the output.
     */
    public Ansi font(Font font, Style ... styles) {
        prepend(codes.color(ColorType.DEFAULT, ColorType.DEFAULT, font, styles));
        append(codes.clearFont(), codes.clear());
        return this;
    }

    /**
     * Sets the color, and optionally the style(s), of the next block of text to
     * display.
     *
     * @param style A standard ANSI style.
     * @param styles Any additional ANSI styles to apply, <em>optional</em>.
     * @return This Ansi instance, to continue modifying the output.
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
     * move the cursor up, positive values move it down.
     *
     * @param lines a number of lines to move the cursor from its current position.
     * @return This Ansi instance, to continue modifying the output.
     */
    public Ansi moveCursor(int lines) {
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
     *
     * @param lines a number of lines to move the cursor from its current position.
     * @param columns a number of columns to move the cursor from its current position.
     * @return This Ansi instance, to continue modifying the output.
     */
    public Ansi moveCursor(int lines, int columns) {
        prepend(codes.moveCursor(lines, columns));
        return this;
    }

    /**
     * Saves the current position of the cursor. Use <code>restoreCursor</code>
     * to move the cursor back to this position.
     *
     * This is <em>not</em> a chainable method, to avoid erroneously calling
     * <code>ansi().restoreCursor();</code> without writing. The restore-cursor
     * code is written to stdout immediately.
     */
    public void saveCursor() {
        checkState(preBuffer.isEmpty() && postBuffer.isEmpty(), "Unnecessary chaining; cannot set additional formatting on the window title.");
        prepend(codes.saveCursor());
        out("");
    }

    /**
     * Restores the cursor to its previously saved position; use in tandem with
     * <code>saveCursor</code>.
     *
     * This is <em>not</em> a chainable method, to avoid erroneously calling
     * <code>ansi().restoreCursor();</code> without writing. The restore-cursor
     * code is written to stdout immediately.
     */
    public void restoreCursor() {
        checkState(preBuffer.isEmpty() && postBuffer.isEmpty(), "Unnecessary chaining; cannot set additional formatting on the window title.");
        prepend(codes.restoreCursor());
        out("");
    }

    /**
     * Moves the cursor to a fixed position on the screen, where the top left
     * corner is 1, 1. Restores the cursor to its original location afterwards.
     *
     * @param row the row to move the cursor to.
     * @param column the column to move the cursor to.
     * @return This Ansi instance, to continue modifying the output.
     */
    public Ansi fixed(int row, int column) {
        prepend(codes.saveCursor(), codes.positionCursor(row, column));
        append(codes.restoreCursor());
        return this;
    }

    /**
     * Clears the current line and positions the cursor at the start of the
     * line.
     *
     * @return This Ansi instance, to continue modifying the output.
     */
    public Ansi overwriteThisLine() {
        prepend(codes.clearLine(), codes.positionCursor(1));
        return this;
    }

    /**
     * Clears the current and previous lines and positions the cursor at the
     * start of the previous line.
     * 
     * <p>As of February 2016 this
     * <a href="https://gitlab.com/gnachman/iterm2/issues/3617">does not work on iTerm2 stable</a>,
     * you must install the beta.
     *
     * @return This Ansi instance, to continue modifying the output.
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

        if (args.length > 0) {
            buffer.append(String.format(text, args));
        } else {
            buffer.append(text);
        }

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
     * <p>Compare to <code>System.out.print()</code>
     *
     * @param text the text to write to stdout, wrapped by any previously-specified ANSI codes.
     * @param args Arguments to use if text contains printf-style tokens, <em>optional</em>.
     * @return a clean Ansi instance, to continue chaining output.
     */
    public Ansi out(String text, Object ... args) {
        return writeToPrintStream(stdout, false, text, args);
    }

    /**
     * Writes text to stdout after piping it and args through String.format(),
     * also prints a newline.
     *
     * <p>Compare to <code>System.out.println()</code>
     *
     * @param text the text to write to stdout, wrapped by any previously-specified ANSI codes.
     * @param args Arguments to use if text contains printf-style tokens, <em>optional</em>.
     * @return a clean Ansi instance, to continue chaining output.
     */
    public Ansi outln(String text, Object ... args) {
        return writeToPrintStream(stdout, true, text, args);
    }

    /**
     * Simply writes a newline to stdout.
     *
     * <p>Compare to <code>System.out.println()</code>
     *
     * @return a clean Ansi instance, to continue chaining output.
     */
    public Ansi outln() {
        return writeToPrintStream(stdout, true, "");
    }

    /**
     * Writes text to stderr after piping it and args through String.format().
     *
     * <p>Compare to <code>System.err.print()</code>
     *
     * @param text the text to write to stderr, wrapped by any previously-specified ANSI codes.
     * @param args Arguments to use if text contains printf-style tokens, <em>optional</em>.
     * @return a clean Ansi instance, to continue chaining output.
     */
    public Ansi err(String text, Object ... args) {
        return writeToPrintStream(stderr, false, text, args);
    }

    /**
     * Writes text to stderr after piping it and args through String.format(),
     * also prints a newline.
     *
     * <p>Compare to <code>System.err.println()</code>
     *
     * @param text the text to write to stderr, wrapped by any previously-specified ANSI codes.
     * @param args Arguments to use if text contains printf-style tokens, <em>optional</em>.
     * @return a clean Ansi instance, to continue chaining output.
     */
    public Ansi errln(String text, Object ... args) {
        return writeToPrintStream(stderr, true, text, args);
    }

    /**
     * Simply writes a newline to stderr.
     *
     * <p>Compare to <code>System.err.println()</code>
     *
     * @return a clean Ansi instance, to continue chaining output.
     */
    public Ansi errln() {
        return writeToPrintStream(stderr, true, "");
    }

    /**
     * Helper method to delay output for a short period of time, so users can
     * see the output changing. Useful when overwriting previous lines.
     *
     * <p>Note this calls <code>Thread.sleep()</code>.
     *
     * @return This Ansi instance, to continue modifying the output.
     */
    public Ansi delay() {
        return delay(DEFAULT_DELAY);
    }

    /**
     * Helper method to delay output for a short period of time, so users can
     * see the output changing. Useful when overwriting previous lines.
     *
     * <p>Note this calls <code>Thread.sleep()</code>.
     *
     * @param millis a number of milliseconds to delay output.
     * @return This Ansi instance, to continue modifying the output.
     */
    public Ansi delay(long millis) {
        try {
            // TODO make this mock-able via AnsiForTests
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // Restore threads interrupted state, the application should handle the interruption
            Thread.currentThread().interrupt();
        }
        return this;
    }
}
