package com.mwdiamond.fansi;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Strings;

import com.mwdiamond.fansi.Ansi.Color;
import com.mwdiamond.fansi.Ansi.Style;

/**
 * High-level utility class for common tasks involving ANSI output.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>Updatable progress bars for {@linkplain #percentProgressBar percentages} or
 *   {@linkplain #counterProgressBar items}</li>
 *   <li>Message Templates:
 *   <ul>
 *     <li>Status messages: {@link #ok [ OK ]}, {@link #warn [ WARN ]}, {@link #error [ ERROR ]}
 *     <li>Event / logging messages: {@link #info [ INFO ]}, {@link #warn [ WARN ]},
 *       {@link #error [ ERROR ]}
 *     <li>Task results: {@link #done [ DONE ]}, {@link #pass [ PASS ]}, {@link #skip [ SKIP ]},
 *       {@link #fail [ FAIL ]}
 *   </ul>
 * </ul>
 *
 * <p>This class can be safely persisted and most usages can simply share one instance across the
 * entire application. Like the {@link Ansi} class individual methods are not thread-safe, however,
 * and external synchronization should be used if multiple threads will be writing output
 * concurrently.
 */
public class AnsiUtils {
  private final AnsiFactory factory;

  /**
   * Creates a new {@code AnsiUtils} instance using the {@link AnsiFactory#DEFAULT DEFAULT} ANSI
   * factory.
   *
   * @return a new {@code AnsiUtils} instance
   */
  public static AnsiUtils create() {
    return create(AnsiFactory.DEFAULT);
  }

  /**
   * Creates a new {@code AnsiUtils} instance which uses the given {@link AnsiFactory}.
   *
   * @param factory the factory to use to construct {@link Ansi} instances used by this class
   * @return a new {@code AnsiUtils} instance
   */
  public static AnsiUtils create(AnsiFactory factory) {
    return new AnsiUtils(factory);
  }

  private AnsiUtils(AnsiFactory factory) {
    this.factory = checkNotNull(factory);
  }

  private Ansi ansi() {
    return factory.ansi();
  }

  /**
   * Writes a message to stdout along with a stylized {@code [ OK ]} block. Generally used to
   * indicate a good status (as opposed to a successful task; prefer {@link #pass pass()} or
   * {@link #done done()} for that).
   *
   * @param message the message to display
   * @param args (optionally) arguments to format into the message
   * @see #warn warn()
   * @see #error error()
   */
  public void ok(String message, Object... args) {
    status("OK   ", Color.GREEN, message, args);
  }

  /**
   * Writes a message to stdout along with a stylized {@code [ DONE ]} block. Generally used to
   * indicate a successfully completed task. Prefer {@link #ok ok()} when reporting a good status.
   *
   * @param message the message to display
   * @param args (optionally) arguments to format into the message
   * @see #pass pass()
   * @see #skip skip()
   * @see #fail fail()
   */
  public void done(String message, Object... args) {
    status("DONE ", Color.GREEN, message, args);
  }

  /**
   * Writes a message to stdout along with a stylized {@code [ PASS ]} block. Generally used to
   * indicate a successfully completed task such as a passing test. Prefer {@link #ok ok()} when
   * reporting a good status.
   *
   * @param message the message to display
   * @param args (optionally) arguments to format into the message
   * @see #done done()
   * @see #skip skip()
   * @see #fail fail()
   */
  public void pass(String message, Object... args) {
    status("PASS ", Color.GREEN, message, args);
  }

  /**
   * Writes a message to stdout along with a stylized {@code [ INFO ]} block. Generally used to
   * inform the user of an expected event.
   *
   * @param message the message to display
   * @param args (optionally) arguments to format into the message
   * @see #warn warn()
   * @see #error error()
   */
  public void info(String message, Object... args) {
    status("INFO ", Color.GREY, message, args);
  }

  /**
   * Writes a message to stdout along with a stylized {@code [ WARN ]} block. Generally used to
   * inform the user of an unexpected but recoverable event or status.
   *
   * @param message the message to display
   * @param args (optionally) arguments to format into the message
   * @see #ok ok()
   * @see #info info()
   * @see #error error()
   */
  public void warn(String message, Object... args) {
    status("WARN ", Color.YELLOW, message, args);
  }

  /**
   * Writes a message to stdout along with a stylized {@code [ SKIP ]} block. Generally used to
   * indicate a task was not attempted in order to avoid failing. Prefer {@link #warn warn()} when
   * reporting an unexpected but recoverable status or event.
   *
   * @param message the message to display
   * @param args (optionally) arguments to format into the message
   * @see #done done()
   * @see #pass pass()
   * @see #fail fail()
   */
  public void skip(String message, Object... args) {
    status("SKIP ", Color.YELLOW, message, args);
  }

  /**
   * Writes a message to stdout along with stylized a {@code [ FAIL ]} block. Generally used to
   * indicate a task failed to complete successfully. Prefer {@link #error error()} when
   * reporting a serious unexpected status or event.
   *
   * @param message the message to display
   * @param args (optionally) arguments to format into the message
   * @see #done done()
   * @see #pass pass()
   * @see #skip skip()
   */
  public void fail(String message, Object... args) {
    status("FAIL ", Color.RED, message, args);
  }

  /**
   * Writes a message to stdout along with a stylized {@code [ ERROR ]} block. Generally used to
   * inform the user of an unexpected status or event that cannot be (easily) recovered from. Prefer
   * {@link #fail fail()} for tasks that have failed.
   *
   * @param message the message to display
   * @param args (optionally) arguments to format into the message
   * @see #info info()
   * @see #warn warn()
   */
  public void error(String message, Object... args) {
    status("ERROR", Color.RED, message, args);
  }

  // TODO should this right-justify the [ status ] after the message
  // https://bitbucket.org/dimo414/f-ansi/issues/7
  private void status(String status, Color color, String message, Object... args) {
    ansi().out("[ ").color(color, Style.BOLD).out(status).out(" ] ").outln(message, args);
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
    
    private ProgressBar(String prefix, String suffix, char bar, String units, int steps) {
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
      updateProgress(step + 1);
    }
    
    /**
     * Set the progress bar's current progress.
     *
     * @param currentStep the number of steps completed (filled on the progress bar)
     * @throws IllegalArgumentException if the current step is not in the range [0, totalSteps]
     */
    public void updateProgress(int currentStep) {
      checkArgument(currentStep >= 0, "Invalid currentStep: %s", currentStep);
      checkArgument(
          currentStep <= this.steps,
          "currentStep (%s) cannot exceed totalSteps (%s)", currentStep, this.steps);
      this.step = currentStep;
      render();
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
      checkArgument(totalSteps > 0, "Invalid totalSteps: %s", totalSteps);
      checkArgument(currentStep >= 0, "Invalid currentStep: %s", currentStep);
      checkArgument(
          currentStep <= totalSteps,
          "currentStep (%s) cannot exceed totalSteps (%s)", currentStep, totalSteps);
      this.step = currentStep;
      this.steps = totalSteps;
      render();
    }
    
    /**
     * Clears the progress bar from the screen leaving the cursor at the beginning of the current
     * line. This is a terminating operation; the progress bar can no longer be updated.
     */
    public void remove() {
      ansi().overwriteThisLine().out("");
      done = true;
    }
    
    /**
     * Fills the progress bar (i.e. {@code updateProgress(totalSteps)}) and moves the cursor to the
     * next line, leaving the completed progress bar on the screen. This is a terminating operation;
     * the progress bar can no longer be updated.
     */
    public void finish() {
      step = steps;
      render();
      ansi().outln();
      done = true;
    }

    /**
     * Returns a string showing the current progress in text (as opposed to graphically).
     *
     * @param currentStep the number of steps completed (filled on the progress bar)
     * @param totalSteps the total number of steps to be completed (width of the progress bar)
     * @return a string representation of the current and total steps, e.g. {@code "15/25"}
     */
    protected abstract String progressAsText(int currentStep, int totalSteps);
    
    private void render() {
      checkState(!done, "Progress bar can no longer be updated; remove() or finish() called.");
      int columns = Ansi.columns();
      
      String suffixAndCount = suffix + " " + progressAsText(step, steps) + units;
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

  /**
   * Returns a {@code ProgressBar} that will display a percentage. Users will generally call
   * {@link ProgressBar#updateProgress} to update the percentage to display, however they can also
   * use {@link ProgressBar#updateSteps} to let the {@code ProgressBar} convert the current step
   * into a percentage.
   *
   * <p>Example:
   *
   * <pre>{@code [=============================                            ] 50%}</pre>
   *
   * @return a {@link ProgressBar} instance starting at 0 percent complete
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
   * <p>Example (prefix: {@code <}, suffix: {@code >}, bar: {@code -}):
   *
   * <pre>{@code <-----------------------------                            > 50%}</pre>
   *
   * @param barPrefix Text to frame the start of the progress bar
   * @param barSuffix Text to frame the tail of the progress bar
   * @param barChar Character used as the progress bar
   *
   * @return a {@link ProgressBar} instance starting at 0 percent complete and using the given
   *     formatting
   */
  public ProgressBar percentProgressBar(String barPrefix, String barSuffix, char barChar) {
    return new PercentProgressBar(barPrefix, barSuffix, barChar);
  }
  
  private class PercentProgressBar extends ProgressBar {
    PercentProgressBar(String prefix, String suffix, char bar) {
      super(prefix, suffix, bar, "%", 100);
    }

    @Override
    protected String progressAsText(int step, int steps) {
      return String.valueOf(steps == 100 ? step : Math.round(100.0f * step / steps));
    }
  }

  /**
   * Returns a {@code ProgressBar} that will display an x/y counter. The {@code initialStepCount}
   * is the number of steps that will need to be taken to fill the bar. It can be updated later via
   * {@link ProgressBar#updateSteps updateSteps()}.
   *
   * <p>Example:
   *
   * <pre>{@code [===========================                           ] 50/100}</pre>
   *
   * @param initialStepCount The number of steps to fill the progress bar
   * @return a {@link ProgressBar} instance starting at 0 steps complete
   */
  public ProgressBar counterProgressBar(int initialStepCount) {
    return counterProgressBar(initialStepCount, "");
  }

  /**
   * Returns a {@code ProgressBar} that will display an x/y counter. The {@code initialStepCount}
   * is the number of steps that will need to be taken to fill the bar. It can be updated later via
   * {@link ProgressBar#updateSteps updateSteps()}.
   *
   * <p>Example (units: {@code " tasks"}):
   *
   * <pre>{@code [========================                        ] 50/100 tasks}</pre>
   *
   * @param initialStepCount The number of steps to fill the progress bar
   * @param units text appended to the counter as the progress bar's units, e.g. "%" or " tasks"
   * @return a {@link ProgressBar} instance starting at 0 steps complete and using the given units
   */
  public ProgressBar counterProgressBar(int initialStepCount, String units) {
    return counterProgressBar("[", "]", '=', initialStepCount, units);
  }

  /**
   * Returns a {@code ProgressBar} that will display an x/y counter with the given configuration.
   * The {@code initialStepCount} is the number of steps that will need to be taken to fill the
   * bar. It can be updated later via {@link ProgressBar#updateSteps updateSteps()}.
   *
   * <p>Example (prefix: {@code <}, suffix: {@code >}, bar: {@code -}, units: {@code " tasks"}):
   *
   * <pre>{@code <------------------------                        > 50/100 tasks}</pre>
   *
   * @param barPrefix Text to frame the start of the progress bar
   * @param barSuffix Text to frame the tail of the progress bar
   * @param barChar Character used as the progress bar
   * @param initialStepCount The number of steps to fill the progress bar
   * @param units text appended to the counter as the progress bar's units, e.g. "%" or " tasks"
   * @return a {@link ProgressBar} instance starting at 0 steps complete and using the given units
   *     and formatting
   */
  public ProgressBar counterProgressBar(
      String barPrefix, String barSuffix, char barChar, int initialStepCount, String units) {
    return new FractionProgressBar(barPrefix, barSuffix, barChar, units, initialStepCount);
  }
  
  private class FractionProgressBar extends ProgressBar {
    FractionProgressBar(String prefix, String suffix, char bar, String units, int steps) {
      super(prefix, suffix, bar, units, steps);
    }

    @Override
    protected String progressAsText(int step, int steps) {
      return step + "/" + steps;
    }
  }
}
