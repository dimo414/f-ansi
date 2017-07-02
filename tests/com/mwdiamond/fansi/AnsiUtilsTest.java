package com.mwdiamond.fansi;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link AnsiUtils}.
 */
public class AnsiUtilsTest {
  private static final String LN = System.lineSeparator();
  /** Clear line, position cursor at start of line. */
  private static final String CL = "\\e[2K\\e[1G";

  private AnsiForTests ansiForTests;
  private AnsiUtils ansiUtils;

  @BeforeMethod
  private void flushAnsi() {
    ansiForTests = new AnsiForTests();
    ansiUtils = AnsiUtils.create(ansiForTests);
  }

  @Test
  @ChangeDetector(timesUpdated = 0)
  public void messages() {
    final ImmutableList<String> expectedLines = ImmutableList.of(
        "[ \\e[1;32mOK   \\e[m ] Foo Bar",
        "[ \\e[1;32mDONE \\e[m ] Foo Bar",
        "[ \\e[1;32mPASS \\e[m ] Foo Bar",
        "[ \\e[1;37mINFO \\e[m ] Foo Bar",
        "[ \\e[1;33mWARN \\e[m ] Foo Bar",
        "[ \\e[1;33mSKIP \\e[m ] Foo Bar",
        "[ \\e[1;31mFAIL \\e[m ] Foo Bar",
        "[ \\e[1;31mERROR\\e[m ] Foo Bar",
        "[ \\e[1;35mDEBUG\\e[m ] Foo Bar"
    );

    ansiUtils.ok("Foo %s", "Bar");
    ansiUtils.done("Foo %s", "Bar");
    ansiUtils.pass("Foo %s", "Bar");
    ansiUtils.info("Foo %s", "Bar");
    ansiUtils.warn("Foo %s", "Bar");
    ansiUtils.skip("Foo %s", "Bar");
    ansiUtils.fail("Foo %s", "Bar");
    ansiUtils.error("Foo %s", "Bar");
    ansiUtils.debug("Foo %s", "Bar");

    assertThat(ansiForTests.getStdout()).isEqualTo(Joiner.on(LN).join(expectedLines) + LN);
    assertThat(ansiForTests.getStderr()).isEmpty();
  }

  @Test
  @ChangeDetector(timesUpdated = 0)
  public void progressBar_percent() {
    final String expected =
          CL + "[                                                                           ] 1%"
        + CL + "[                                                                           ] 0%"
        + CL + "[=======                                                                   ] 10%"
        + CL + "[=================================================                         ] 67%"
        + CL + "[=========================================================================] 100%"
        + LN;

    AnsiUtils.ProgressBar progressBar = ansiUtils.percentProgressBar();
    assertThat(ansiForTests.getStdout()).isEqualTo("");
    progressBar.increment();
    progressBar.updateProgress(0);
    progressBar.updateProgress(10);
    progressBar.updateSteps(10, 15);
    progressBar.finish();

    assertThat(ansiForTests.getStdout()).isEqualTo(expected);
    assertThat(ansiForTests.getStderr()).isEmpty();
  }

  @Test
  @ChangeDetector(timesUpdated = 0)
  public void progressBar_counter() {
    final String expected =
          CL + "[=======                                                                  ] 1/10"
        + CL + "[                                                                         ] 0/10"
        + CL + "[=====================                                                    ] 3/10"
        + CL + "[================================================                        ] 10/15"
        + CL + "[========================================================================] 15/15"
        + LN;

    AnsiUtils.ProgressBar progressBar = ansiUtils.counterProgressBar(10);
    assertThat(ansiForTests.getStdout()).isEqualTo("");
    progressBar.increment();
    progressBar.updateProgress(0);
    progressBar.updateProgress(3);
    progressBar.updateSteps(10, 15);
    progressBar.finish();

    assertThat(ansiForTests.getStdout()).isEqualTo(expected);
    assertThat(ansiForTests.getStderr()).isEmpty();
  }

  @Test
  public void progressBar_systemWidth() {
    ansiForTests = new AnsiForTests(
        Codes.RAW, new AnsiForTests.SystemInfoForTests(null, 40));
    ansiUtils = AnsiUtils.create(ansiForTests);

    AnsiUtils.ProgressBar progressBar = ansiUtils.percentProgressBar();
    progressBar.updateSteps(10, 15);

    assertThat(ansiForTests.getStdout())
        .isEqualTo(CL + "[======================            ] 67%");
  }

  @Test
  public void progressBar_remove() {
    AnsiUtils.ProgressBar progressBar = ansiUtils.percentProgressBar();
    progressBar.updateSteps(10, 15);
    progressBar.remove();

    assertThat(ansiForTests.getStdout()).isEqualTo(
        CL + "[=================================================                         ] 67%"
            + CL);
  }

  @Test
  public void progressBar_percent_formatting() {
    AnsiUtils.ProgressBar progressBar = ansiUtils.percentProgressBar("<{", ">}", '*');
    progressBar.updateSteps(10, 15);

    assertThat(ansiForTests.getStdout()).isEqualTo(
        CL + "<{************************************************                        >} 67%");
  }

  @Test
  public void progressBar_counter_units() {
    AnsiUtils.ProgressBar progressBar = ansiUtils.counterProgressBar(3, " tasks");
    progressBar.increment();

    assertThat(ansiForTests.getStdout()).isEqualTo(
        CL + "[======================                                              ] 1/3 tasks");
  }

  @Test
  public void progressBar_counter_formatting() {
    AnsiUtils.ProgressBar progressBar = ansiUtils.counterProgressBar("<{", ">}", '*', 3, " tasks");
    progressBar.updateSteps(10, 15);

    assertThat(ansiForTests.getStdout()).isEqualTo(
        CL + "<{******************************************                      >} 10/15 tasks");
  }

  @Test
  public void progressBar_alreadyFinished() {
    AnsiUtils.ProgressBar progressBar = ansiUtils.percentProgressBar();
    progressBar.finish();

    IllegalStateException e;
    e = Assert.expectThrows(IllegalStateException.class, () -> progressBar.updateProgress(0));
    assertThat(e).hasMessageThat().contains("can no longer be updated");

    e = Assert.expectThrows(IllegalStateException.class, progressBar::finish);
    assertThat(e).hasMessageThat().contains("can not be finished");

    e = Assert.expectThrows(IllegalStateException.class, progressBar::remove);
    assertThat(e).hasMessageThat().contains("can not be removed");
  }

  @Test
  public void progressBar_alreadyRemoved() {
    AnsiUtils.ProgressBar progressBar = ansiUtils.percentProgressBar();
    progressBar.remove();

    IllegalStateException e;
    e = Assert.expectThrows(IllegalStateException.class, progressBar::increment);
    assertThat(e).hasMessageThat().contains("can no longer be updated");

    e = Assert.expectThrows(IllegalStateException.class, progressBar::finish);
    assertThat(e).hasMessageThat().contains("can not be finished");

    e = Assert.expectThrows(IllegalStateException.class, progressBar::remove);
    assertThat(e).hasMessageThat().contains("can not be removed");
  }

  @Test
  public void progressBar_invalidValues() {
    AnsiUtils.ProgressBar progressBar = ansiUtils.percentProgressBar();

    Assert.expectThrows(IllegalArgumentException.class, () -> progressBar.updateSteps(0, 0));
    Assert.expectThrows(IllegalArgumentException.class, () -> progressBar.updateSteps(-1, 10));
    Assert.expectThrows(IllegalArgumentException.class, () -> progressBar.updateSteps(10, 5));
    Assert.expectThrows(IllegalArgumentException.class, () -> progressBar.updateSteps(-5, -10));

    progressBar.updateSteps(5, 5);

    Assert.expectThrows(IllegalStateException.class, progressBar::increment);

    Assert.expectThrows(IllegalArgumentException.class, () -> progressBar.updateProgress(-1));
    Assert.expectThrows(IllegalStateException.class, () -> progressBar.updateProgress(6));

    progressBar.updateProgress(0);

    progressBar.finish();
  }
}
