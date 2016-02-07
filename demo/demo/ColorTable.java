package demo;

import static com.mwdiamond.fansi.Ansi.ansi;
import static com.mwdiamond.fansi.Ansi.Color.*;

import java.util.ArrayList;
import java.util.Arrays;

import com.mwdiamond.fansi.Ansi.Color;
import com.mwdiamond.fansi.Ansi.Font;
import com.mwdiamond.fansi.Ansi.Style;

public class ColorTable {
    private static final String CELL = "%8.8s";
    private static final String LABEL = CELL + " ";


    public static void main(String[] args) {
        Font font = Font.DEFAULT;
        ArrayList<Style> styles = new ArrayList<>();

        for (String a : args) {
            try {
                font = Font.valueOf(a.toUpperCase());
            } catch (IllegalArgumentException discard) {
                try {
                    styles.add(Style.valueOf(a.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    ansi().color(RED).err("Error:").errln(" Unknown font/style %s", a)
                        .errln("\tValid fonts: %s", Arrays.toString(Font.values()))
                        .errln("\tValid styles: %s", Arrays.toString(Style.values()));
                    System.exit(1);
                }
            }
        }

        table(font, styles.toArray(new Style[styles.size()]));
    }

    private static String shorten(Color color) {
        return color.toString().replace("LIGHT_", "L_").replace("DARK_", "D_");
    }

    private static void table(Font font, Style[] styles) {
        // Header
        ansi().out(CELL, "");
        for (Color background : Color.values()) {
            ansi().out(LABEL, shorten(background));
        }
        ansi().outln();

        // Rows
        for (Color color : Color.values()) {
            ansi().out(LABEL, shorten(color));
            for (Color background : Color.values()) {
                ansi().color(color, background, font, styles).out(CELL, "Text ").out(" ");
            }
            ansi().outln();
        }
    }
}
