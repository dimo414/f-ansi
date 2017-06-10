package demo;

import static com.mwdiamond.fansi.Ansi.ansi;

/**
 * Prints several tables displaying the color indicies; first a simple grid of the indicies in
 * order, then split into logical groups.
 *
 * <p>Credit to http://stackoverflow.com/a/27165165/113632 for inspiring the grouped output.
 */
public class ColorIndexTable {
  private static final int BLACK = 0x10;
  private static final int WHITE = 0xE7;

  public static void main(String[] args) {
    ansi().outln("All codes as Hex Values:");
    for (int i = 0; i < 256; i++) {
      ansi().background(i).out("%3s ", Integer.toHexString(i).toUpperCase());
      if (i % 0x10 == 0xF) {
        ansi().outln();
      }
    }


    ansi().outln().outln("Codes by Group");
    ansi().outln("Named colors, 0x00-0x0F");
    for (int i = 0x00; i < 0x10; i++) {
      ansi().background(i).out("   ");
      if (i % 0x08 == 0x07) {
        ansi().outln();
      }
    }

    ansi().outln().outln("RGB colors, each 0-5, 0x10-0xE7");
    // Loop order is strange (g, r, b) so that the codes are output
    // in blocks, where each block is a continuous set of values.
    for (int row = 0; row < 2; row++) {
      ansi().outln("  0x%X-0x%-21X   0x%X-0x%-21X   0x%X-0x%X",
          0x10 + row * 0x6C, 0x33 + row * 0x6C,
          0x34 + row * 0x6C, 0x57 + row * 0x6C,
          0x58 + row * 0x6C, 0x7B + row * 0x6C);
      for (int green = 0; green < 6; green++) {
        for (int column = 0; column < 3; column++) {
          int red = column + row * 3;
          for (int blue = 0; blue < 6; blue++) {
            int textColor = red + green + blue < 0x08 ? WHITE : BLACK;
            ansi().color(textColor, toCode(red, green, blue)).out(" %s%s%s ", red, green, blue);
          }
          ansi().out(" ");
        }
        ansi().outln();
      }
      ansi().outln();
    }

    ansi().outln("Greyscale colors, 1-24, 0xE8-0xFF");
    for (int i = 0xE8; i < 0x100; i++) {
      ansi().color(i < 0xf4 ? WHITE : BLACK, i).out(" %2s ", i - 0xE7);
    }
    ansi().outln();
  }

  private static int toCode(int red, int green, int blue) {
    return 0x10 + 0x24 * red + 0x06 * green + blue;
  }
}
