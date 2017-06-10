package com.mwdiamond.fansi;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableMap;

import com.mwdiamond.fansi.Ansi.Color;
import com.mwdiamond.fansi.Ansi.Font;
import com.mwdiamond.fansi.Ansi.Style;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Map.Entry;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Basic unit tests of Ansi, including (sadly) a number of change detector-style tests that may be
 * brittle.
 */
public class AnsiTest {
  private static final String HELLO = "Hello World";
  private static final String LN = System.lineSeparator();
  private AnsiForTests ansiForTests;

  /**
   * Annotation indicating a test method is a change-detector, verifying behavior that may not be
   * part of the public API, and therefore could change. When these methods need to be updated
   * increment the timesUpdated field as a record of how often this occurs. A high update count is a
   * good indication that a test should be refactored or removed.
   */
  @Target({ElementType.METHOD})
  private @interface ChangeDetector {
    int timesUpdated();
  }

  @BeforeMethod
  private void flushAnsi() {
    ansiForTests = new AnsiForTests();
  }

  private Ansi ansi() {
    return ansiForTests.ansi();
  }

  @Test
  public void plain() {
    ansi().out(HELLO);
    assertThat(ansiForTests.getStdout()).isEqualTo(HELLO);
    assertThat(ansiForTests.getStderr()).isEmpty();
  }

  @Test
  public void plainLn() {
    ansi().outln(HELLO);
    assertThat(ansiForTests.getStdout()).isEqualTo(HELLO + LN);
    assertThat(ansiForTests.getStderr()).isEmpty();
  }

  @Test
  public void plainErr() {
    ansi().err(HELLO);
    assertThat(ansiForTests.getStdout()).isEmpty();
    assertThat(ansiForTests.getStderr()).isEqualTo(HELLO);
  }

  @Test
  public void plainErrLn() {
    ansi().errln(HELLO);
    assertThat(ansiForTests.getStdout()).isEmpty();
    assertThat(ansiForTests.getStderr()).isEqualTo(HELLO + LN);
  }

  private static final Map<java.awt.Color, Integer> COLORS_TO_COLOR_INDEX =
      new ImmutableMap.Builder<java.awt.Color, Integer>()
          .put(java.awt.Color.WHITE,  0xE7)
          .put(java.awt.Color.GRAY,   0xF4)
          .put(java.awt.Color.BLACK,  0x10)
          .put(java.awt.Color.RED,    0xC4)
          .put(java.awt.Color.PINK,   0xD9)
          .put(java.awt.Color.ORANGE, 0xD6)
          .build();

  @Test
  public void toColorIndex() {
    for (Entry<java.awt.Color, Integer> e : COLORS_TO_COLOR_INDEX.entrySet()) {
      assertWithMessage("Passed Color %s", e.getKey()).that(Ansi.toColorIndex(e.getKey()))
          .isEqualTo(e.getValue());
    }
  }

  @Test
  @ChangeDetector(timesUpdated = 1)
  public void setTitle() {
    ansi().title(HELLO);
    assertThat(ansiForTests.getStdout()).isEqualTo("\\e]2;" + HELLO + "\\a");
  }

  @Test
  @ChangeDetector(timesUpdated = 0)
  public void color() {
    ansi().color(Color.RED).out(HELLO);
    assertThat(ansiForTests.getStdout()).isEqualTo("\\e[31m" + HELLO + "\\e[m");
  }

  @Test
  @ChangeDetector(timesUpdated = 0)
  public void colorBackground() {
    ansi().color(Color.GREEN, Color.RED).out(HELLO);
    assertThat(ansiForTests.getStdout()).isEqualTo("\\e[32;41m" + HELLO + "\\e[m");
  }

  @Test
  @ChangeDetector(timesUpdated = 0)
  public void colorStyle() {
    ansi().color(Color.YELLOW, Style.BOLD).out(HELLO);
    assertThat(ansiForTests.getStdout()).isEqualTo("\\e[1;33m" + HELLO + "\\e[m");
  }

  @Test
  @ChangeDetector(timesUpdated = 0)
  public void colorStyles() {
    ansi().color(Color.CYAN, Style.BOLD, Style.ITALIC).out(HELLO);
    assertThat(ansiForTests.getStdout()).isEqualTo("\\e[1;3;36m" + HELLO + "\\e[m");
  }

  @Test
  @ChangeDetector(timesUpdated = 0)
  public void colorFont() {
    ansi().color(Color.BLUE, Font.F1).out(HELLO);
    assertThat(ansiForTests.getStdout()).isEqualTo("\\e[11;34m" + HELLO + "\\e[10m\\e[m");
  }

  @Test
  public void chainedColor() {
    ansi().color(Color.BLUE).out(HELLO).color(Color.RED, Color.GREEN).out(HELLO);
    assertThat(ansiForTests.getStdout())
        .isEqualTo("\\e[34m" + HELLO + "\\e[m\\e[31;42m" + HELLO + "\\e[m");
  }

  @Test
  @ChangeDetector(timesUpdated = 1)
  public void color8Bit() {
    ansi().color(100, 200).out(HELLO);
    assertThat(ansiForTests.getStdout()).isEqualTo("\\e[38;5;100;48;5;200m" + HELLO + "\\e[m");
  }

  @Test
  @ChangeDetector(timesUpdated = 1)
  public void color24Bit() {
    ansi().color(java.awt.Color.RED, java.awt.Color.BLUE).out(HELLO);
    assertThat(ansiForTests.getStdout())
        .isEqualTo("\\e[38;2;255;0;0;48;2;0;0;255m" + HELLO + "\\e[m");
  }

  @Test
  @ChangeDetector(timesUpdated = 0)
  public void moveCursor() {
    ansi().moveCursor(10).out(HELLO);
    assertThat(ansiForTests.getStdout()).isEqualTo("\\e[10E" + HELLO);
  }

  @Test
  @ChangeDetector(timesUpdated = 0)
  public void moveCursorColumns() {
    ansi().moveCursor(10, 10).out(HELLO);
    assertThat(ansiForTests.getStdout()).isEqualTo("\\e[10B\\e[10C" + HELLO);
  }

  @Test
  @ChangeDetector(timesUpdated = 0)
  public void saveRestoreCursor() {
    ansi().saveCursor();
    ansi().restoreCursor();
    assertThat(ansiForTests.getStdout()).isEqualTo("\\e[s\\e[u");
  }

  @Test
  @ChangeDetector(timesUpdated = 0)
  public void fixed() {
    ansi().fixed(10, 20).out(HELLO);
    assertThat(ansiForTests.getStdout()).isEqualTo("\\e[s\\e[10;20H" + HELLO + "\\e[u");
  }

  @Test
  public void fixedColor() {
    ansi().fixed(10, 20).color(Color.RED).outln(HELLO);
    ansi().color(Color.RED).fixed(10, 20).outln(HELLO);
    assertThat(ansiForTests.getStdout()).isEqualTo(
        "\\e[s\\e[10;20H\\e[31m" + HELLO + "\\e[m\\e[u" + LN
        + "\\e[31m\\e[s\\e[10;20H" + HELLO + "\\e[u\\e[m" + LN);
  }

  @Test
  @ChangeDetector(timesUpdated = 1)
  public void overwriteThisLine() {
    ansi().overwriteThisLine().out(HELLO);
    assertThat(ansiForTests.getStdout()).isEqualTo("\\e[2K\\e[1G" + HELLO);
  }

  @Test
  @ChangeDetector(timesUpdated = 0)
  public void overwriteLastLine() {
    ansi().overwriteLastLine().out(HELLO);
    assertThat(ansiForTests.getStdout()).isEqualTo("\\e[2K\\e[1F\\e[2K" + HELLO);
  }
}
