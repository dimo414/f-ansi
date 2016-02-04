package com.mwdiamond.fansi;

import static com.google.common.base.Preconditions.checkState;

import java.io.PrintStream;
import java.util.LinkedList;
public class Ansi {
    // Might make this dynamic in the future, based on the startup environment
    private static final Codes DEFAULT_CODES = Codes.REAL;

    public static Ansi ansi() {
        return new Ansi(DEFAULT_CODES);
    }

    public static Ansi realAnsi() {
        return new Ansi(Codes.REAL);
    }

    public static Ansi rawAnsi() {
        return new Ansi(Codes.RAW);
    }

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
    }

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

    public void title(String title) {
        checkState(preBuffer.isEmpty() && postBuffer.isEmpty(), "Unnecessary chaining; cannot set additional formatting on the window title.");
        out(codes.title(title));
    }

    public Ansi color(Color color, Style ... styles) {
        prepend(codes.color(color, Color.DEFAULT, Font.DEFAULT, styles));
        append(codes.clear());
        return this;
    }

    public Ansi color(Color color, Font font, Style ... styles) {
        prepend(codes.color(color, Color.DEFAULT, font, styles));
        append(codes.clearFont(), codes.clear());
        return this;
    }

    public Ansi color(Color color, Color background, Style ... styles) {
        prepend(codes.color(color, background, Font.DEFAULT, styles));
        append(codes.clear());
        return this;
    }

    public Ansi color(Color color, Color background, Font font, Style ... styles) {
        prepend(codes.color(color, background, font, styles));
        append(codes.clearFont(), codes.clear());
        return this;
    }

    public Ansi background(Color background, Style ... styles) {
        prepend(codes.color(Color.DEFAULT, background, Font.DEFAULT, styles));
        append(codes.clear());
        return this;
    }

    public Ansi font(Font font, Style ... styles) {
        prepend(codes.color(Color.DEFAULT, Color.DEFAULT, font, styles));
        append(codes.clearFont(), codes.clear());
        return this;
    }

    public Ansi style(Style ... styles) {
        prepend(codes.color(Color.DEFAULT, Color.DEFAULT, Font.DEFAULT, styles));
        append(codes.clear());
        return this;
    }

    public Ansi moveCursor(int lines) {
        prepend(codes.saveCursor());
        if (lines < 0) {
            prepend(codes.upLine(0 - lines));
        } else {
            prepend(codes.downLine(lines)); // will raise an exception if 0
        }
        return this;
    }

    public Ansi moveCursor(int lines, int columns) {
        prepend(codes.saveCursor(), codes.moveCursor(lines, columns));
        return this;
    }

    public Ansi fixed(int row, int column) {
        prepend(codes.saveCursor(), codes.positionCursor(row, column));
        append(codes.restoreCursor());
        return this;
    }

    public Ansi restoreCursor() {
        prepend(codes.restoreCursor());
        return this;
    }

    public Ansi overwriteThisLine() {
        prepend(codes.clearLine());
        return this;
    }

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

    public Ansi out(String text, Object ... args) {
        return writeToPrintStream(stdout, false, text, args);
    }

    public Ansi outln(String text, Object ... args) {
        return writeToPrintStream(stdout, true, text, args);
    }

    public Ansi outln() {
        return writeToPrintStream(stdout, true, "");
    }

    public Ansi err(String text, Object ... args) {
        return writeToPrintStream(stderr, false, text, args);
    }

    public Ansi errln(String text, Object ... args) {
        return writeToPrintStream(stderr, true, text, args);
    }

    public Ansi errln() {
        return writeToPrintStream(stderr, true, "");
    }
}
