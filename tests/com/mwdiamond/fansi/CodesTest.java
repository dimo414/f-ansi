package com.mwdiamond.fansi;

import static com.google.common.truth.Truth.assertThat;

import org.testng.annotations.Test;

/**
 * Bare-bones tests of Codes; most of this behavior is verified by AnsiTest.
 *
 * <p>Generally just test the static instances behave as expected.
 */
public class CodesTest {
  @Test
  public void realCodes() {
    assertThat(Codes.REAL.moveCursor(1, 1)).startsWith("\u001B");
  }

  @Test
  public void rawCodes() {
    assertThat(Codes.RAW.moveCursor(1, 1)).startsWith("\\e[");
  }

  @Test
  public void noOpCodes() {
    assertThat(Codes.NO_OP.moveCursor(1, 1)).isEmpty();
  }
}
