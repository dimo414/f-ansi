package com.mwdiamond.fansi;


import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class Ansi {    
    public static Ansi ansi() {
        return new Ansi();
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
        
        private int color() {
            return code;
        }
        
        private int background() {
            return color() + BACKGROUND;
        }
    }
    
    public static enum Style {
        BOLD(1), DIM(2), ITALIC(3), UNDERLINE(4), BLINK(5), BLINK_RAPID(6), REVERSE(7), CONCEAL(8), STRIKETHROUGH(9),
        FONT_DEFAULT(10), FONT_1(11), FONT_2(12), FONT_3(13), FONT_4(14), FONT_5(15), FONT_6(16), FONT_7(17), FONT_8(18), FONT_9(19), FONT_FRAKTUR(20),
        FRAME(51), ENCIRCLE(52), OVERLINE(53);
        
        private final int code;
        
        private Style(int code) {
            this.code = code; 
        }
        
        public int code() {
            return code;
        }
    }
    
    private final StringBuffer preBuffer;
    private final StringBuffer postBuffer;
    
    private Ansi() {
        preBuffer = new StringBuffer();
        postBuffer = new StringBuffer();
    }
    
    public Ansi color(Color color, Style ... styles) {
        preBuffer.append(Codes.color(color, Color.DEFAULT, styles));
        postBuffer.append(Codes.clear());
        return this;
    }
    
    public Ansi color(Color color, Color background, Style ... styles) {
        preBuffer.append(Codes.color(color, background, styles));
        postBuffer.append(Codes.clear());
        return this;
    }
    
    public Ansi background(Color background, Style ... styles) {
       preBuffer.append(Codes.color(Color.DEFAULT, background, styles));
       postBuffer.append(Codes.clear());
       return this;
    }
    
    public Ansi style(Style ... styles) {
        preBuffer.append(Codes.color(Color.DEFAULT, Color.DEFAULT, styles));
        postBuffer.append(Codes.clear());
        return this;
    }
    
    public Ansi moveCursor(int lines) {
        preBuffer.append(Codes.saveCursor());
        if (lines < 0) {
            preBuffer.append(Codes.upLine(0 - lines));
        } else {
            preBuffer.append(Codes.downLine(lines)); // will raise an exception if 0
        }
        return this;
    }
    
    public Ansi moveCursor(int lines, int columns) {
        preBuffer.append(Codes.saveCursor())
            .append(Codes.moveCursor(lines, columns));
        return this;
    }
    
    public Ansi fixed(int row, int column) {
        preBuffer.append(Codes.saveCursor())
            .append(Codes.positionCursor(row, column));
        postBuffer.append(Codes.restoreCursor());
        return this;
    }
    
    public Ansi restoreCursor() {
        preBuffer.append(Codes.restoreCursor());
        return this;
    }
    
    public Ansi overwriteThisLine() {
        preBuffer.append(Codes.clearLine());
        return this;
    }
    
    public Ansi overwriteLastLine() {
        preBuffer.append(Codes.clearLine())
            .append(Codes.upLine(1))
            .append(Codes.clearLine());
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
        return writeToPrintStream(System.out, false, text, args);
    }
    
    public Ansi outln(String text, Object ... args) {
        return writeToPrintStream(System.out, true, text, args);
    }
    
    public Ansi err(String text, Object ... args) {
        return writeToPrintStream(System.err, false, text, args);
    }
    
    public Ansi errln(String text, Object ... args) {
        return writeToPrintStream(System.err, true, text, args);
    }
    
    /**
     * Direct implementation of the ANSI codes, as listed on
     * https://en.wikipedia.org/wiki/ANSI_escape_code
     */
    private static class Codes {
        private static String ESC_REAL = "\u001B";
        private static String ESC_RAW = "\\e";
        private static String CSI_REAL = ESC_REAL + "[";
        private static String CSI_RAW = ESC_RAW + "[";
        private static String CSI = CSI_REAL;
        
        private static String CUU = "A";
        private static String CUD = "B";
        private static String CUF = "C";
        private static String CUB = "D";
        private static String CNL = "E";
        private static String CPL = "F";
        @SuppressWarnings("unused") private static String CHA = "G";
        private static String CUP = "H";
        private static String ED = "J";
        private static String EL = "K";
        private static String SU = "S";
        private static String SD = "T";
        @SuppressWarnings("unused") private static String HVP = "f";
        private static String SGR = "m";
        private static String DSR = "6n";
        private static String SCP = "s";
        private static String RCP = "u";
        private static String DECTCEM_HIDE = "?25l";
        private static String DECTCEM_SHOW = "?25h";
        
        private static String SEPARATOR = ";";
        
        public static String moveCursor(int lines, int columns) {
            StringBuilder buffer = new StringBuilder();
            if(lines > 0) {
                buffer.append(CSI).append(lines).append(CUD);
            } else if (lines < 0) {
                buffer.append(CSI).append(0 - lines).append(CUU);
            }
            if (columns > 0) {
                buffer.append(CSI).append(columns).append(CUF);
            } else if (columns < 0) {
                buffer.append(CSI).append(0 - columns).append(CUB);
            }
            return buffer.toString();
        }
        
        public static String positionCursor(int row, int column) {
            checkArgument(row > 0, "Must specify a positive row, was %s", row);
            checkArgument(column > 0, "Must specify a positive column, was %s", column);
            return CSI + row + ";" + column + CUP;
        }
        
        public static String saveCursor() {
            return CSI + SCP;
        }
        
        public static String restoreCursor() {
            return CSI + RCP;
        }

        public static String downLine(int n) {
            checkArgument(n > 0, "Must specify a positive number of lines, was %s", n);
            return CSI + n + CNL;
        }

        public static String upLine(int n) {
            checkArgument(n > 0, "Must specify a positive number of lines, was %s", n);
            return CSI + n + CPL;
        }
        
        public static String clearDisplay() {
            return CSI + 2 + ED;
        }
        
        public static String clearDisplayForward() {
            return CSI + 0 + ED;
        }
        
        public static String clearDisplayBackward() {
            return CSI + 1 + ED;
        }
        
        public static String clearLine() {
            return CSI + 2 + EL;
        }
        
        public static String clearLineForward() {
            return CSI + 0 + EL;
        }
        
        public static String clearLineBackward() {
            return CSI + 1 + EL;
        }
        
        public static String color(Color color, Color background, Style ... styles) {
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
            StringBuilder output = new StringBuilder(CSI);
            for (String part : parts.subList(0, parts.size()-1)) {
                output.append(part).append(SEPARATOR);
            }
            return output.append(parts.get(parts.size()-1)).append(SGR).toString();
        }
        
        public static String clear() {
            return CSI + SGR;
        }
    }
    
    // TODO replace with Preconditions.checkArgument if Guava gets included
    private static void checkArgument(boolean test, String message, Object ... args) {
        if (!test) {
            throw new IllegalArgumentException(String.format(message, args));
        }
    }
}
