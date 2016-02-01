package demo;

import static com.mwdiamond.fansi.Ansi.*;

import com.mwdiamond.fansi.Ansi.Color;
import com.mwdiamond.fansi.Ansi.Style;

public class FAnsiDemo {
    public static void main(String[] args) throws Exception {
        ansi().title("Look, I set the title!");
        
        ansi().color(Color.RED).out("Hello").out(" - ").color(Color.LIGHT_GREEN).outln("%s", "World");
        ansi().color(Color.MAGENTA, Color.YELLOW, Style.BOLD).outln("Uuugly...");
        ansi().out("[ ").color(Color.GREEN).out("OK").outln(" ] %s", "A Long Message");
        ansi().outln("Waiting...").overwriteLastLine().outln(slowCommand());
        slowCommand();
        ansi().overwriteLastLine().background(Color.RED).outln("Nevermind...");
        System.out.println();
        for (Style s : Style.values()) {
            ansi().style(s).out("%s", s).out(" ");
        }
        ansi().outln("");
        
        for (Color c : Color.values()) {
            ansi().color(c).out("%s", c).out(" ").background(c).out("%s", c).out(" ");
        }
        ansi().outln("");
        
        ansi().color(Color.RED).fixed(30, 80).out("Look it's a message in space!");
        
        ansi().color(Color.GREEN, Style.BOLD, Style.ITALIC, Style.UNDERLINE).outln("There's a lot going on here...");
        
        ansi().color(Color.RED).fixed(30, 80).out("Look it's a message in space!");
    }
    
    private static String slowCommand() throws Exception {
        Thread.sleep(2000);
        return "Success!";
    }
}
