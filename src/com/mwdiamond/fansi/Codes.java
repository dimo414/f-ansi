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
class Codes {
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
    @SuppressWarnings("unused") private static final String CHA = "G";
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

    // Known implementations
    public static final Codes REAL = new Codes(ESC_REAL, BELL_REAL);
    public static final Codes RAW = new Codes(ESC_RAW, BELL_RAW);

    private final String esc;
    private final String bell;
    private final String csi;

    private Codes(String esc, String bell) {
        this.esc = esc;
        this.bell = bell;
        this.csi = esc + CSI_CHAR;
    }

    // http://www.tldp.org/HOWTO/Bash-Prompt-HOWTO/xterm-title-bar-manipulations.html
    public String title(String text) {
        return esc + TITLE_ESCAPE + text + bell;
    }

    public String promptQuote(String text) {
        return BEGIN_NON_PRINTING + text + END_NON_PRINTING;
    }

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

    public String downLine(int n) {
        checkArgument(n > 0, "Must specify a positive number of lines, was %s", n);
        return csi + n + CNL;
    }

    public String upLine(int n) {
        checkArgument(n > 0, "Must specify a positive number of lines, was %s", n);
        return csi + n + CPL;
    }

    public String positionCursor(int row, int column) {
        checkArgument(row > 0, "Must specify a positive row, was %s", row);
        checkArgument(column > 0, "Must specify a positive column, was %s", column);
        return csi + row + ";" + column + CUP;
    }

    public String clearDisplay() {
        return csi + 2 + ED;
    }

    public String clearDisplayForward() {
        return csi + 0 + ED;
    }

    public String clearDisplayBackward() {
        return csi + 1 + ED;
    }

    public String clearLine() {
        return csi + 2 + EL;
    }

    public String clearLineForward() {
        return csi + 0 + EL;
    }

    public String clearLineBackward() {
        return csi + 1 + EL;
    }

    public String scrollUp(int n) {
        checkArgument(n > 0, "Must specify a positive number of lines, was %s", n);
        return csi + n + SU;
    }

    public String scrollDown(int n) {
        checkArgument(n > 0, "Must specify a positive number of lines, was %s", n);
        return csi + n + SD;
    }

    private List<Object> getColorCodeParts(ColorType color) {
        if (color.namedColor() != null) {
            return ImmutableList.<Object>of(color.namedColor().color());
        } else if (color.colorIndex() != null) {
            return Color.extended(color.colorIndex(), false);
        } else if (color.javaColor() != null) {
            return Color.extended(color.javaColor(), false);
        }
        throw new IllegalArgumentException("Unexected ColorType, " + color);
    }

    private List<Object> getBackgroundCodeParts(ColorType color) {
        if (color.namedColor() != null) {
            return ImmutableList.<Object>of(color.namedColor().background());
        } else if (color.colorIndex() != null) {
            return Color.extended(color.colorIndex(), true);
        } else if (color.javaColor() != null) {
            return Color.extended(color.javaColor(), true);
        }
        throw new IllegalArgumentException("Unexected ColorType, " + color);
    }

    public String color(ColorType color, ColorType background, Font font, Style ... styles) {
        List<Object> parts = new ArrayList<>();
        for (Style s : styles) {
            parts.add(s.code());
        }
        if (font != Font.DEFAULT) {
            parts.add(font.code());
        }
        if (color.namedColor() != Color.DEFAULT) {
            parts.addAll(getColorCodeParts(color));
        }
        if (background.namedColor() != Color.DEFAULT) {
            parts.addAll(getBackgroundCodeParts(background));
        }
        if(parts.isEmpty()) {
            return "";
        }
        return SEPARATOR_JOINER.appendTo(new StringBuilder(csi), parts).append(SGR).toString();
    }

    public String clearFont() {
        return csi + Font.DEFAULT.code() + SGR;
    }

    public String clear() {
        return csi + SGR;
    }

    public String getCursor() {
        return csi + DSR;
    }

    public String saveCursor() {
        return csi + SCP;
    }

    public String restoreCursor() {
        return csi + RCP;
    }

    public String hideCursor() {
        return csi + DECTCEM_HIDE;
    }

    public String showCursor() {
        return csi + DECTCEM_SHOW;
    }

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
    }
}