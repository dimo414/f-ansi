package com.mwdiamond.fansi;

import java.util.ArrayList;
import java.util.List;

import com.mwdiamond.fansi.Ansi.Color;
import com.mwdiamond.fansi.Ansi.Style;

/**
 * Direct implementation of the ANSI codes, as listed on
 * https://en.wikipedia.org/wiki/ANSI_escape_code
 * 
 * Subclasses can implement alternative codes or disable
 * methods they don't support via UnsupportedOperationException.
 * 
 * This class has no state, and is therefore thread-safe.
 * Subclasses should similarly avoid introducing any state.
 */
class Codes {
    private static final String ESC_REAL = "\u001B";
    private static final String BELL_REAL = "\u0007";
    private static final String ESC_RAW = "\\e";
    private static final String BELL_RAW = "\\a";
    private static String CSI_CHAR = "[";
    private static String TITLE_ESCAPE = "]0;";
    
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
    
    public String title(String text) {
        return esc + TITLE_ESCAPE + text + bell;
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
    
    public String positionCursor(int row, int column) {
        checkArgument(row > 0, "Must specify a positive row, was %s", row);
        checkArgument(column > 0, "Must specify a positive column, was %s", column);
        return csi + row + ";" + column + CUP;
    }
    
    public String saveCursor() {
        return csi + SCP;
    }
    
    public String restoreCursor() {
        return csi + RCP;
    }

    public String downLine(int n) {
        checkArgument(n > 0, "Must specify a positive number of lines, was %s", n);
        return csi + n + CNL;
    }

    public String upLine(int n) {
        checkArgument(n > 0, "Must specify a positive number of lines, was %s", n);
        return csi + n + CPL;
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
    
    public String color(Color color, Color background, Style ... styles) {
        List<String> parts = new ArrayList<>();
        for (Style s : styles) {
            parts.add(String.valueOf(s.code()));
        }
        if (color != Color.DEFAULT) {
            parts.add(String.valueOf(color.color()));
        }
        if (background != Color.DEFAULT) {
            parts.add(String.valueOf(background.background()));
        }
        if(parts.isEmpty()) {
            return "";
        }
        // TODO replace with a Joiner if we include Guava as a dependency
        //return SEPARATOR_JOINER.appendTo(new StringBuilder(CSI), parts).append(SGR).toString();
        StringBuilder output = new StringBuilder(csi);
        for (String part : parts.subList(0, parts.size()-1)) {
            output.append(part).append(SEPARATOR);
        }
        return output.append(parts.get(parts.size()-1)).append(SGR).toString();
    }
    
    public String clear() {
        return csi + SGR;
    }
    
    // TODO replace with Preconditions.checkArgument if Guava gets included
    private static void checkArgument(boolean test, String message, Object ... args) {
        if (!test) {
            throw new IllegalArgumentException(String.format(message, args));
        }
    }
}