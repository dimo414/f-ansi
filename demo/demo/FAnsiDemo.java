package demo;

import static com.mwdiamond.fansi.Ansi.*;

public class FAnsiDemo {
    public static void main(String[] args) throws Exception {
        ansi().title("Look, I set the title!");

        ansi().color(Color.RED).out("Hello").out(" - ").color(Color.LIGHT_GREEN).outln("%s", "World");
        ansi().color(Color.MAGENTA, Color.YELLOW, Style.BOLD).outln("Uuugly...");
        ansi().color(100).out("Color Index").out(" ").color(java.awt.Color.ORANGE).outln("Java Color");
        ansi().out("[ ").color(Color.GREEN).out("OK").outln(" ] %s", "A Long Message");
        ansi().outln("Waiting...").overwriteLastLine().outln(slowCommand());
        slowCommand();
        ansi().overwriteLastLine().background(Color.RED).outln("Nevermind...");

        ansi().outln();
        for (Color c : Color.values()) {
            ansi().color(c).out("%s", c).out(" ").background(c).out("%s", c).out(" ");
        }
        ansi().outln();

        ansi().outln();
        for (Style s : Style.values()) {
            ansi().style(s).out("%s", s).out(" ");
        }
        ansi().outln();

        ansi().outln().style(Style.UNDERLINE, Style.BOLD, Style.BLINK).outln("Multiple styles");

        ansi().outln();
        for (Font f : Font.values()) {
            ansi().font(f).out("%s", f).out(" ");
        }
        ansi().outln().outln();

        ansi().color(Color.GREEN, Style.BOLD, Style.ITALIC, Style.UNDERLINE).outln("There's a lot going on here...");

        ansi().color(Color.RED).fixed(30, 80).out("Look it's a message in space!");
    }

    private static String slowCommand() throws Exception {
        Thread.sleep(2000);
        return "Success!";
    }
}
