package com.mwdiamond.fansi;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Strings;

import com.mwdiamond.fansi.Ansi.Color;

// Not yet public; see https://bitbucket.org/dimo414/f-ansi/issues/8
// Don't forget to add unit tests before publishing
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

  /**
   * A text progress bar that will overwrite itself when updated, presenting the user with dynamic
   * yet concise visualization of the application's current progress.
   * 
   * <p>This class is not thread-safe; multi-threaded applications should generally have a single
   * thread responsible for output, otherwise access to this class needs to be
   * {@code synchronized}. 
   */
  public abstract class ProgressBar {

    private final String prefix;
    private final String suffix;
    private final String bar;
    private final String units;
    
    private int step;
    private int steps;
    private boolean done = false;
    
    ProgressBar(String prefix, String suffix, char bar, String units, int steps) {
      this.prefix = checkNotNull(prefix);
      this.suffix = checkNotNull(suffix);
      this.bar = String.valueOf(bar);
      this.units = checkNotNull(units);
      checkArgument(steps > 0);
      this.steps = steps;
    }
    
    /**
     * Increment the progress bar by one step.
     * 
     * @throws IllegalStateException if the current step is already the final step (i.e. 100%)
     */
    public void increment() {
      checkState(step < steps);
      updateProgress(step + 1);
    }
    
    /**
     * Set the progress bar's current progress.
     * 
     * @throws IllegalArgumentException if the current step is not in the range [0, totalSteps]
     */
    public void updateProgress(int currentStep) {
      checkArgument(currentStep >= 0 && currentStep <= steps);
      this.step = currentStep;
      display();
    }
    
    /**
     * Updates the progress bar, setting both the current number of steps and the total number of
     * steps that will be completed.
     * 
     * @param currentStep the number of steps completed (filled on the progress bar)
     * @param totalSteps the total number of steps to be completed (width of the progress bar)
     * 
     * @throws IllegalArgumentException if the current step is not in the range [0, totalSteps] or
     *     totalSteps is not positive.
     */
    public void updateSteps(int currentStep, int totalSteps) {
      checkArgument(totalSteps > 0 && currentStep >= 0 && currentStep <= totalSteps);
      this.step = currentStep;
      this.steps = totalSteps;
      display();
    }
    
    /**
     * Clears the progress bar from the screen, leaving the cursor at the beginning of the current
     * line.
     */
    public void clear() {
      ansi().overwriteThisLine().out("");
      done = true;
    }
    
    /**
     * Fills the progress bar (i.e. {@code updateProgress(totalSteps)}) and moves the cursor to the
     * next line, leaving the completed progress bar on the screen.
     */
    public void finished() {
      step = steps;
      display();
      ansi().outln();
      done = true;
    }
    
    protected abstract String format(int currentStep, int totalSteps);
    
    private void display() {
      checkState(!done, "Progress bar can no longer be updated; clear() or finished() called.");
      int columns = 80;
      // TODO use $COLUMNS or some other dynamic value for width
      // https://bitbucket.org/dimo414/f-ansi/issues/7
      
      String suffixAndCount = suffix + " " + format(step, steps) + units;
      int barWidth = columns - (prefix.length() + suffixAndCount.length());
      if (barWidth < 4) { // 25% per char
        ansi().overwriteThisLine().out(step + units);
        return;
      }
      // round down so progress bar doesn't look done too early
      int progress = barWidth * step / steps;
      ansi()
          .overwriteThisLine()
          .out(prefix)
          .out(Strings.repeat(bar, progress))
          .out(Strings.repeat(" ", barWidth - progress))
          .out(suffixAndCount);
    }
  }
  
  private class PercentProgressBar extends ProgressBar {
    PercentProgressBar(String prefix, String suffix, char bar) {
      super(prefix, suffix, bar, "%", 100);
    }

    @Override
    protected String format(int step, int steps) {
      return String.valueOf(steps == 100 ? step : Math.round(100.0f * step / steps));
    }
  }
  
  private class FractionProgressBar extends ProgressBar {
    FractionProgressBar(String prefix, String suffix, char bar, String units, int steps) {
      super(prefix, suffix, bar, units, steps);
    }

    @Override
    protected String format(int step, int steps) {
      return step + "/" + steps;
    }
  }

  /**
   * Returns a {@code ProgressBar} that will display a percentage. Users will generally call
   * {@link ProgressBar#updateProgress} to update the percentage to display, however they can also
   * use {@link ProgressBar#updateSteps} to let the {@code ProgressBar} convert the current step
   * into a percentage.
   */
  public ProgressBar percentProgressBar() {
    return percentProgressBar("[", "]", '=');
  }

  /**
   * Returns a {@code ProgressBar} that will display a percentage. Users will generally call
   * {@link ProgressBar#updateProgress} to update the percentage to display, however they can also
   * use {@link ProgressBar#updateSteps} to let the {@code ProgressBar} convert the current step
   * into a percentage.
   * 
   * @param barPrefix Text to frame the start of the progress bar
   * @param barSuffix Text to frame the tail of the progress bar
   * @param barChar Character used as the progress bar
   */
  public ProgressBar percentProgressBar(String barPrefix, String barSuffix, char barChar) {
    return new PercentProgressBar(barPrefix, barSuffix, barChar);
  }

  /**
   * Returns a {@code ProgressBar} that will display an x/y counter. The {@code initialStepCount}
   * is the number of steps that will need to be taken to fill the bar. It can be updated later via
   * {@link ProgressBar#updateSteps}.
   * 
   * @param initialStepCount The number of steps to fill the progress bar
   */
  public ProgressBar counterProgressBar(int initialStepCount) {
    return counterProgressBar(initialStepCount, "");
  }

  /**
   * Returns a {@code ProgressBar} that will display an x/y counter. The {@code initialStepCount}
   * is the number of steps that will need to be taken to fill the bar. It can be updated later via
   * {@link ProgressBar#updateSteps}.
   * 
   * @param initialStepCount The number of steps to fill the progress bar
   * @param units text appended to the counter as the progress bar's units, e.g. "%" or " tasks"
   */
  public ProgressBar counterProgressBar(int initialStepCount, String units) {
    return counterProgressBar("[", "]", '=', initialStepCount, units);
  }
  
  /**
   * Returns a {@code ProgressBar} that will display an x/y counter with the given configuration.
   * The {@code initialStepCount} is the number of steps that will need to be taken to fill the
   * bar. It can be updated later via {@link ProgressBar#updateSteps}.
   * 
   * @param barPrefix Text to frame the start of the progress bar
   * @param barSuffix Text to frame the tail of the progress bar
   * @param barChar Character used as the progress bar
   * @param initialStepCount The number of steps to fill the progress bar
   * @param units text appended to the counter as the progress bar's units, e.g. "%" or " tasks"
   */
  public ProgressBar counterProgressBar(
      String barPrefix, String barSuffix, char barChar, int initialStepCount, String units) {
    return new FractionProgressBar(barPrefix, barSuffix, barChar, units, initialStepCount);
  }

  // TODO move to demo.AnsiUtilsDemo once public
  public static void main(String[] args) throws InterruptedException {
    AnsiUtils ansiUtils = new AnsiUtils();

    ansiUtils.ok("Job '%s' succeeded", "Demo");
    ansiUtils.warn("Job '%s' disabled", "Misbehaving");
    ansiUtils.fail("Job '%s' failed to start", "Broken");
    
    ProgressBar progress = ansiUtils.percentProgressBar();
    for (int i = 0; i <= 100; i++) {
      progress.updateProgress(i);
      Thread.sleep(25);
    }
    progress.finished();
    
    ProgressBar tasks = ansiUtils.counterProgressBar(1, " tasks");
    for (int i = 0; i <= 300; i ++) {
      if (i % 100 == 1) {
        tasks.updateSteps(i, i + 99);
      } else {
        tasks.updateProgress(i);
        Thread.sleep(20);
      }
    }
    Thread.sleep(100);
    tasks.clear();
    ansiUtils.ok("All tasks complete.");
  }
}
