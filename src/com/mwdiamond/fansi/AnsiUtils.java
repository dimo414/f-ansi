package com.mwdiamond.fansi;

import static com.google.common.base.Preconditions.checkNotNull;

import com.mwdiamond.fansi.Ansi.Color;

// Not yet public; see https://bitbucket.org/dimo414/f-ansi/issues/8
class AnsiUtils {
  private final AnsiFactory factory;

  public AnsiUtils() {
    factory = AnsiFactory.DEFAULT;
  }

  public AnsiUtils(AnsiFactory factory) {
    this.factory = checkNotNull(factory);
  }

  private Ansi ansi() {
    return factory.ansi();
  }

  // TODO these should right-justify the [ status ]
  // https://bitbucket.org/dimo414/f-ansi/issues/7
  public void ok(String message, Object... args) {
    ansi().out("[ ").color(Color.GREEN).out("OK").out("   ] ").outln(message, args);
  }

  public void warn(String message, Object... args) {
    ansi().out("[ ").color(Color.YELLOW).out("WARN").out(" ] ").outln(message, args);
  }

  public void fail(String message, Object... args) {
    ansi().out("[ ").color(Color.RED).out("FAIL").out(" ] ").outln(message, args);
  }


  // TODO move to demo.AnsiUtilsDemo once public
  public static void main(String[] args) {
    AnsiUtils ansiUtils = new AnsiUtils();

    ansiUtils.ok("Job '%s' succeeded", "Demo");
    ansiUtils.warn("Job '%s' disabled", "Misbehaving");
    ansiUtils.fail("Job '%s' failed to start", "Broken");
  }
}
