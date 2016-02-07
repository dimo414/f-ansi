package demo;

import static com.mwdiamond.fansi.Ansi.ansi;

public class ColorIndexTable {
    public static void main(String[] args) {
        for (int i = 0; i < 256; i++) {
            ansi().background(i).out("%3s ", Integer.toHexString(i).toUpperCase());
            if (i % 0x10 == 0xF) ansi().outln();
        }
    }
}
