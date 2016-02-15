package demo;

import static com.mwdiamond.fansi.Ansi.ansi;

import java.io.IOException;

public class CursorActions {
    private static final int LINES = 10;
    public static void main(String[] args) throws IOException, InterruptedException {
        clearLines();

        ansi().saveCursor();
        for (int i = 1; i <= 4; i++) {
            ansi().moveCursor(-i).delay().out("Move cursor: %s", i);
        }
        for (int i = 1; i <= 4; i++) {
            ansi().moveCursor(i, i).delay().out("Move cursor: %s %s", i, i);
        }
        ansi().restoreCursor();

        for (int i = 1; i <= 10; i++) {
            ansi().delay().fixed(i, i).out("Fixed: %s %s", i, i);
        }

        clearLines();

        ansi().out("A message to overwrite").delay(500).overwriteThisLine().outln("Overwritten").delay(500);
        ansi().outln("A message on a previous line to overwrite").out("And this line").delay(500).overwriteLastLine().outln("Overwritten");
    }

    private static void clearLines() {
        for (int i = 0; i < LINES; i++) {
            System.out.println();
        }
    }
}
