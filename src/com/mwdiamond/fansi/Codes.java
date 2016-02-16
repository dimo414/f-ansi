package com.mwdiamond.fansi;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.mwdiamond.fansi.Ansi.Color;
import com.mwdiamond.fansi.Ansi.Font;
import com.mwdiamond.fansi.Ansi.Style;

/**
 * Direct implementation of the ANSI codes, as listed on
 * https://en.wikipedia.org/wiki/ANSI_escape_code
 * http://www.tldp.org/HOWTO/Bash-Prompt-HOWTO/bash-prompt-escape-sequences.html
 * 
 * Subclasses can implement alternative codes or disable
 * methods they don't support via UnsupportedOperationException.
 * 
 * This class has no state, and is therefore thread-safe.
 * Subclasses should similarly avoid introducing any state.
 */
interface Codes {

    // Known implementations
    public static final Codes REAL = AnsiCodes.real();
    public static final Codes RAW = AnsiCodes.raw();
    public static final Codes NO_OP = new NoOpCodes();

    String title(String text);

    // TODO unused?
    String promptQuote(String text);

    String moveCursor(int lines, int columns);

    String downLine(int n);

    String upLine(int n);

    String positionCursor(int column);

    String positionCursor(int row, int column);

    String clearDisplay();

    String clearDisplayForward();

    String clearDisplayBackward();

    String clearLine();

    String clearLineForward();

    String clearLineBackward();

    String scrollUp(int n);

    String scrollDown(int n);

    String color(ColorType color, ColorType background, Font font, Style ... styles);

    String clearFont();

    String clear();

    String getCursor();

    String saveCursor();

    String restoreCursor();

    String hideCursor();

    String showCursor();

    static class ColorType {
        static ColorType DEFAULT = new ColorType(Color.DEFAULT);

        private Color namedColor;
        private Integer colorIndex;
        private java.awt.Color javaColor;

        ColorType(Color color) {
            namedColor = color;
        }

        ColorType(int color) {
            checkArgument(color >= 0 && color <= 255,
                "Must specify a color index within the range [0, 255], was: %s", color);
            colorIndex = color;
        }

        ColorType(java.awt.Color color) {
            javaColor = color;
        }

        Color namedColor() {
            return namedColor;
        }

        Integer colorIndex() {
            return colorIndex;
        }

        java.awt.Color javaColor() {
            return javaColor;
        }

        List<Object> getColorCodeParts() {
            if (namedColor() != null) {
                return ImmutableList.<Object>of(namedColor().color());
            } else if (colorIndex() != null) {
                return Color.extended(colorIndex(), false);
            } else if (javaColor() != null) {
                return Color.extended(javaColor(), false);
            }
            throw new IllegalArgumentException("Unexected ColorType, " + this);
        }

        List<Object> getBackgroundCodeParts() {
            if (namedColor() != null) {
                return ImmutableList.<Object>of(namedColor().background());
            } else if (colorIndex() != null) {
                return Color.extended(colorIndex(), true);
            } else if (javaColor() != null) {
                return Color.extended(javaColor(), true);
            }
            throw new IllegalArgumentException("Unexected ColorType, " + this);
        }
    }

    static class AnsiCodes implements Codes {
        // Escapes
        private static final String ESC_REAL = "\u001B";
        private static final String BELL_REAL = "\u0007";
        private static final String ESC_RAW = "\\e";
        private static final String BELL_RAW = "\\a";
        private static final String TITLE_ESCAPE = "]0;";
        private static final String BEGIN_NON_PRINTING = "\\[";
        private static final String END_NON_PRINTING = "\\]";

        // CSI
        private static final String CSI_CHAR = "[";
        private static final String CUU = "A";
        private static final String CUD = "B";
        private static final String CUF = "C";
        private static final String CUB = "D";
        private static final String CNL = "E";
        private static final String CPL = "F";
        private static final String CHA = "G";
        private static final String CUP = "H";
        private static final String ED = "J";
        private static final String EL = "K";
        private static final String SU = "S";
        private static final String SD = "T";
        @SuppressWarnings("unused") private static final String HVP = "f";
        private static final String SGR = "m";
        private static final String DSR = "6n";
        private static final String SCP = "s";
        private static final String RCP = "u";
        private static final String DECTCEM_HIDE = "?25l";
        private static final String DECTCEM_SHOW = "?25h";

        private static String SEPARATOR = ";";
        private static Joiner SEPARATOR_JOINER = Joiner.on(SEPARATOR);

        private final String esc;
        private final String bell;
        private final String csi;

        private AnsiCodes(String esc, String bell) {
            this.esc = esc;
            this.bell = bell;
            this.csi = esc + CSI_CHAR;
        }

        public static AnsiCodes real() {
            return new AnsiCodes(ESC_REAL, BELL_REAL);
        }

        public static AnsiCodes raw() {
            return new AnsiCodes(ESC_RAW, BELL_RAW);
        }

        // http://www.tldp.org/HOWTO/Bash-Prompt-HOWTO/xterm-title-bar-manipulations.html
        @Override
        public String title(String text) {
            return esc + TITLE_ESCAPE + text + bell;
        }

        @Override
        public String promptQuote(String text) {
            return BEGIN_NON_PRINTING + text + END_NON_PRINTING;
        }

        @Override
        public String moveCursor(int lines, int columns) {
            StringBuilder buffer = new StringBuilder();
            if(lines > 0) {
                buffer.append(csi).append(lines).append(CUD);
            } else if (lines < 0) {
                buffer.append(csi).append(0 - lines).append(CUU);
            }
            if (columns > 0) {
                buffer.append(csi).append(columns).append(CUF);
            } else if (columns < 0) {
                buffer.append(csi).append(0 - columns).append(CUB);
            }
            return buffer.toString();
        }

        @Override
        public String downLine(int n) {
            checkArgument(n > 0, "Must specify a positive number of lines, was %s", n);
            return csi + n + CNL;
        }

        @Override
        public String upLine(int n) {
            checkArgument(n > 0, "Must specify a positive number of lines, was %s", n);
            return csi + n + CPL;
        }

        @Override
        public String positionCursor(int column) {
            checkArgument(column > 0, "Must specify a positive column, was %s", column);
            return csi + column + CHA;
        }

        @Override
        public String positionCursor(int row, int column) {
            checkArgument(row > 0, "Must specify a positive row, was %s", row);
            checkArgument(column > 0, "Must specify a positive column, was %s", column);
            return csi + row + ";" + column + CUP;
        }

        @Override
        public String clearDisplay() {
            return csi + 2 + ED;
        }

        @Override
        public String clearDisplayForward() {
            return csi + 0 + ED;
        }

        @Override
        public String clearDisplayBackward() {
            return csi + 1 + ED;
        }

        @Override
        public String clearLine() {
            return csi + 2 + EL;
        }

        @Override
        public String clearLineForward() {
            return csi + 0 + EL;
        }

        @Override
        public String clearLineBackward() {
            return csi + 1 + EL;
        }

        @Override
        public String scrollUp(int n) {
            checkArgument(n > 0, "Must specify a positive number of lines, was %s", n);
            return csi + n + SU;
        }

        @Override
        public String scrollDown(int n) {
            checkArgument(n > 0, "Must specify a positive number of lines, was %s", n);
            return csi + n + SD;
        }

        @Override
        public String color(ColorType color, ColorType background, Font font, Style ... styles) {
            List<Object> parts = new ArrayList<>();
            for (Style s : styles) {
                parts.add(s.code());
            }
            if (font != Font.DEFAULT) {
                parts.add(font.code());
            }
            if (color.namedColor() != Color.DEFAULT) {
                parts.addAll(color.getColorCodeParts());
            }
            if (background.namedColor() != Color.DEFAULT) {
                parts.addAll(background.getBackgroundCodeParts());
            }
            if(parts.isEmpty()) {
                return "";
            }
            return SEPARATOR_JOINER.appendTo(new StringBuilder(csi), parts).append(SGR).toString();
        }

        @Override
        public String clearFont() {
            return csi + Font.DEFAULT.code() + SGR;
        }

        @Override
        public String clear() {
            return csi + SGR;
        }

        @Override
        public String getCursor() {
            return csi + DSR;
        }

        @Override
        public String saveCursor() {
            return csi + SCP;
        }

        @Override
        public String restoreCursor() {
            return csi + RCP;
        }

        @Override
        public String hideCursor() {
            return csi + DECTCEM_HIDE;
        }

        @Override
        public String showCursor() {
            return csi + DECTCEM_SHOW;
        }
    }

    static class NoOpCodes implements Codes {

        @Override
        public String title(String text) {
            return "";
        }

        @Override
        public String promptQuote(String text) {
            return "";
        }

        @Override
        public String moveCursor(int lines, int columns) {
            return "";
        }

        @Override
        public String downLine(int n) {
            return "";
        }

        @Override
        public String upLine(int n) {
            return "";
        }

        @Override
        public String positionCursor(int column) {
            return "";
        }

        @Override
        public String positionCursor(int row, int column) {
            return "";
        }

        @Override
        public String clearDisplay() {
            return "";
        }

        @Override
        public String clearDisplayForward() {
            return "";
        }

        @Override
        public String clearDisplayBackward() {
            return "";
        }

        @Override
        public String clearLine() {
            return "";
        }

        @Override
        public String clearLineForward() {
            return "";
        }

        @Override
        public String clearLineBackward() {
            return "";
        }

        @Override
        public String scrollUp(int n) {
            return "";
        }

        @Override
        public String scrollDown(int n) {
            return "";
        }

        @Override
        public String color(ColorType color, ColorType background, Font font, Style... styles) {
            return "";
        }

        @Override
        public String clearFont() {
            return "";
        }

        @Override
        public String clear() {
            return "";
        }

        @Override
        public String getCursor() {
            return "";
        }

        @Override
        public String saveCursor() {
            return "";
        }

        @Override
        public String restoreCursor() {
            return "";
        }

        @Override
        public String hideCursor() {
            return "";
        }

        @Override
        public String showCursor() {
            return "";
        }
        
    }
}