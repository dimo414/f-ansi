package com.mwdiamond.fansi;


import java.io.PrintStream;
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
    private final StringBuffer preBuffer;
    private final StringBuffer postBuffer;

    private Ansi(Codes codes) {
        this(System.out, System.err, codes);
    }

    // Package-visible for AnsiForTests
    Ansi(PrintStream stdout, PrintStream stderr, Codes codes) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.codes = codes;
        preBuffer = new StringBuffer();
        postBuffer = new StringBuffer();
    }

    public void title(String title) {
        if(preBuffer.length() > 0 || postBuffer.length() > 0) {
            throw new IllegalStateException("Unnecessary chaining; cannot set additional formatting on the window title.");
        }
        out(codes.title(title));
    }

    public Ansi color(Color color, Style ... styles) {
        preBuffer.append(codes.color(color, Color.DEFAULT, Font.DEFAULT, styles));
        postBuffer.append(codes.clear());
        return this;
    }

    public Ansi color(Color color, Color background, Style ... styles) {
        preBuffer.append(codes.color(color, background, Font.DEFAULT, styles));
        postBuffer.append(codes.clear());
        return this;
    }

    public Ansi color(Color color, Color background, Font font, Style ... styles) {
        preBuffer.append(codes.color(color, background, font, styles));
        postBuffer.append(codes.clearFont()).append(codes.clear());
        return this;
    }

    public Ansi background(Color background, Style ... styles) {
       preBuffer.append(codes.color(Color.DEFAULT, background, Font.DEFAULT, styles));
       postBuffer.append(codes.clear());
       return this;
    }

    public Ansi font(Font font, Style ... styles) {
        preBuffer.append(codes.color(Color.DEFAULT, Color.DEFAULT, font, styles));
        postBuffer.append(codes.clearFont()).append(codes.clear());
        return this;
    }

    public Ansi style(Style ... styles) {
        preBuffer.append(codes.color(Color.DEFAULT, Color.DEFAULT, Font.DEFAULT, styles));
        postBuffer.append(codes.clear());
        return this;
    }

    public Ansi moveCursor(int lines) {
        preBuffer.append(codes.saveCursor());
        if (lines < 0) {
            preBuffer.append(codes.upLine(0 - lines));
        } else {
            preBuffer.append(codes.downLine(lines)); // will raise an exception if 0
        }
        return this;
    }

    public Ansi moveCursor(int lines, int columns) {
        preBuffer.append(codes.saveCursor())
            .append(codes.moveCursor(lines, columns));
        return this;
    }

    public Ansi fixed(int row, int column) {
        preBuffer.append(codes.saveCursor())
            .append(codes.positionCursor(row, column));
        postBuffer.insert(0, codes.restoreCursor());
        return this;
    }

    public Ansi restoreCursor() {
        preBuffer.append(codes.restoreCursor());
        return this;
    }

    public Ansi overwriteThisLine() {
        preBuffer.append(codes.clearLine());
        return this;
    }

    public Ansi overwriteLastLine() {
        preBuffer.append(codes.clearLine())
            .append(codes.upLine(1))
            .append(codes.clearLine());
        return this;
    }

    private Ansi writeToPrintStream(PrintStream out, boolean newLine, String text, Object ... args) {
        preBuffer.append(String.format(text, args)).append(postBuffer);

        if (newLine) {
            out.println(preBuffer);
        } else {
            out.print(preBuffer);
        }

        preBuffer.setLength(0);
        postBuffer.setLength(0);
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
