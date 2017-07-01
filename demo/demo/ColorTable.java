package demo;

import static com.mwdiamond.fansi.Ansi.ansi;

import com.google.common.collect.ImmutableList;

import com.mwdiamond.fansi.Ansi.Color;
import com.mwdiamond.fansi.Ansi.Font;
import com.mwdiamond.fansi.Ansi.Style;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Prints a table of the standard colors and backgrounds.
 *
 * <p>You can specify a font and/or one or more styles as command-line arguments to display the
 * table in the specified format. For instance passing "bold" and "underline" as arguments will
 * print the table in bolded, underlined style.
 */
public class ColorTable {
  private static final String CELL = "%8.8s";
  private static final String LABEL = CELL + " ";
  private static final List<Color> COLORS;

  static {
    ImmutableList.Builder<Color> builder = ImmutableList.builder();
    for (Color color : Color.values()) {
      if (color != Color.MAGENTA && color != Color.LIGHT_MAGENTA) {
        builder.add(color);
      }
    }
    COLORS = builder.build();
  }

  /** main method - see class docs. */
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
          ansi().color(Color.RED).err("Error:").errln(" Unknown font/style %s", a)
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
    ansi().out(LABEL, "");
    for (Color background : COLORS) {
      ansi().out(LABEL, shorten(background));
    }
    ansi().outln();

    // Rows
    for (Color color : COLORS) {
      ansi().out(LABEL, shorten(color));
      for (Color background : COLORS) {
        ansi().color(color, background, font, styles).out(CELL, "Text ").out(" ");
      }
      ansi().outln();
    }
  }
}
