package demo;

import static com.mwdiamond.fansi.Ansi.ansi;

import com.mwdiamond.fansi.AnsiUtils;

/**
 * Demonstrations of {@link AnsiUtils} features.
 */
public class UtilsDemo {
  private static final AnsiUtils ansiUtils = AnsiUtils.create();

  /** main method - see class docs. */
  public static void main(String[] args) throws Exception {
    Thread.sleep(1000);

    ansiUtils.ok("Service '%s' is accepting requests.", "Foo");
    ansiUtils.warn("Service '%s' is rejecting new request.", "Overloaded");
    ansiUtils.error("Service '%s' is not responding.", "Broken");
    ansi().outln();
    ansiUtils.info("Starting task");
    ansiUtils.warn("Task is taking too long");
    ansiUtils.fail("Task was killed");
    ansi().outln();
    ansiUtils.done("Task %d of %d complete", 1, 3);
    ansiUtils.pass("Test %d of %d Passed", 1, 1);
    ansiUtils.skip("Task %d of %d Skipped", 2, 3);
    ansiUtils.fail("Task %d of %d Crashed", 3, 3);
    ansi().outln();

    AnsiUtils.ProgressBar progress = ansiUtils.percentProgressBar();
    for (int i = 0; i <= 100; i++) {
      progress.updateProgress(i);
      Thread.sleep(25);
    }
    progress.finish();

    AnsiUtils.ProgressBar tasks = ansiUtils.counterProgressBar(1, " tasks");
    for (int i = 0; i < 3; i ++) {
      tasks.updateSteps(i * 100, (i + 1) * 100);
      for (int j = 0; j < 100; j++) {
        tasks.updateProgress(i * 100 + j);
        Thread.sleep(20);
      }
    }
    Thread.sleep(100);
    tasks.remove();
    ansiUtils.done("All tasks complete.");
  }
}
