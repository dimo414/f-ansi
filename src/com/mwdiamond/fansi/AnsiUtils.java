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
 *       {@link #error [ ERROR ]}, {@link #debug [ DEBUG ]}
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
   * @see #debug debug()
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
   * @see #debug debug()
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
   * @see #debug debug()
   */
  public void error(String message, Object... args) {
    status("ERROR", Color.RED, message, args);
  }

  /**
   * Writes a message to stdout along with a stylized {@code [ DEBUG ]} block. Generally used to
   * provide additional diagnostic information about the state of the application.
   *
   * @param message the message to display
   * @param args (optionally) arguments to format into the message
   * @see #info info()
   * @see #warn warn()
   * @see #warn error()
   */
  public void debug(String message, Object... args) {
    status("DEBUG", Color.PURPLE, message, args);
  }

  // TODO should this right-justify the [ status ] after the message
  // https://github.com/dimo414/f-ansi/issues/7
  private void status(String status, Color color, String message, Object... args) {
    ansi().out("[ ").color(color, Style.BOLD).out(status).out(" ] ").outln(message, args);
  }

  /** Functional interface to describe the progress bar textually, rather than visually. */
  private interface TextProgress {
    TextProgress FRACTION_PROGRESS = new TextProgress() {
      @Override
      public String progressAsText(int step, int steps) {
        return step + "/" + steps;
      }
    };

    TextProgress PERCENT_PROGRESS = new TextProgress() {
      @Override
      public String progressAsText(int step, int steps) {
        return String.valueOf(steps == 100 ? step : Math.round(100.0 * step / steps)) + "%";
      }
    };

    /**
     * Returns a string showing the current progress in text (as opposed to graphically).
     *
     * @param currentStep the number of steps completed (filled on the progress bar)
     * @param totalSteps the total number of steps to be completed (width of the progress bar)
     * @return a string representation of the current and total steps, e.g. {@code "15/25"}
     */
    String progressAsText(int currentStep, int totalSteps);
  }

  /**
   * A text progress bar that will overwrite itself when updated, presenting the user with dynamic
   * yet concise visualization of the application's current progress.
   * 
   * <p>This class is not thread-safe; multi-threaded applications should generally have a single
   * thread responsible for output, otherwise access to this class needs to be
   * {@code synchronized}. 
   */
  public static class ProgressBar {
    private final AnsiFactory factory;
    private final String prefix;
    private final String suffix;
    private final String bar;
    private final String units;
    private final TextProgress textProgress;
    
    private int step;
    private int steps;
    private boolean done = false;

    private ProgressBar(AnsiFactory factory, Builder builder) {
      this.factory = checkNotNull(factory);
      prefix = builder.prefix;
      suffix = builder.suffix;
      bar = builder.bar;
      units = builder.units;
      textProgress = builder.textProgress;
      steps = builder.steps;
    }

    /**
     * Increment the progress bar by one step.
     * 
     * @throws IllegalStateException if the progress bar is already finished (i.e. 100% or n/n)
     */
    public void increment() {
      updateProgress(step + 1);
    }
    
    /**
     * Set the progress bar's current progress.
     *
     * @param currentStep the number of steps completed (filled on the progress bar)
     * @throws IllegalArgumentException if currentStep is negative
     * @throws IllegalStateException if currentStep is not in the range [0, totalSteps]
     */
    public void updateProgress(int currentStep) {
      checkArgument(currentStep >= 0, "Invalid currentStep: %s", currentStep);
      checkState(
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
     * @throws IllegalArgumentException if currentStep is negative, totalSteps is not positive, or
     *     currentStep is not in the range [0, totalSteps]
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
      checkState(!done, "Progress bar can not be removed; remove() or finish() already called.");
      factory.ansi().overwriteThisLine().out("");
      done = true;
    }
    
    /**
     * Fills the progress bar (i.e. {@code updateProgress(totalSteps)}) and moves the cursor to the
     * next line, leaving the completed progress bar on the screen. This is a terminating operation;
     * the progress bar can no longer be updated.
     */
    public void finish() {
      checkState(!done, "Progress bar can not be finished; remove() or finish() already called.");
      step = steps;
      render();
      factory.ansi().outln();
      done = true;
    }
    
    private void render() {
      checkState(!done, "Progress bar can no longer be updated; remove() or finish() called.");
      int columns = factory.ansi().columns();
      
      String suffixAndCount = suffix + " " + textProgress.progressAsText(step, steps) + units;
      int barWidth = columns - (prefix.length() + suffixAndCount.length());
      if (barWidth < 4) { // 25% per char
        factory.ansi().overwriteThisLine().out(step + units);
        return;
      }
      // round down so progress bar doesn't look done too early
      int progress = barWidth * step / steps;
      factory.ansi()
          .overwriteThisLine()
          .out(prefix)
          .out(Strings.repeat(bar, progress))
          .out(Strings.repeat(" ", barWidth - progress))
          .out(suffixAndCount);
    }

    /**
     * Builder for custom {@link ProgressBar} instances.
     */
    public static class Builder {
      private final AnsiFactory factory;
      private String prefix = "[";
      private String suffix = "]";
      private String bar = "=";
      private String units = "";

      private TextProgress textProgress;
      private int steps;

      private Builder(AnsiFactory factory) {
        this.factory = checkNotNull(factory);
      }

      /**
       * Specify the text to frame the start of the progress bar.
       *
       * @param prefix text to appear before the progress bar
       * @return this builder
       */
      public Builder prefix(String prefix) {
        this.prefix = checkNotNull(prefix);
        return this;
      }

      /**
       * Specify the text to frame the end of the progress bar.
       *
       * @param suffix text to appear after the progress bar
       * @return this builder
       */
      public Builder suffix(String suffix) {
        this.suffix = checkNotNull(suffix);
        return this;
      }

      /**
       * Specify the character used as the progress bar.
       *
       * @param barChar character to use in the progress bar
       * @return this builder
       */
      public Builder barCharacter(char barChar) {
        this.bar = String.valueOf(barChar);
        return this;
      }

      /**
       * Text appended to the counter as the progress bar's units, e.g. "KB" or " tasks".
       *
       * @param units the units of progress to display
       * @return this builder
       */
      public Builder units(String units) {
        this.units = checkNotNull(units);
        return this;
      }

      /**
       * Returns a {@code ProgressBar} that will display a percentage. Users will generally call
       * {@link ProgressBar#updateProgress} to update the percentage to display, however they can
       * also use {@link ProgressBar#updateSteps} to let the {@code ProgressBar} convert the current
       * step into a percentage.
       *
       * <p>This method simply constructs the {@code ProgressBar}, nothing is written to the
       * console.
       *
       * @return a progress bar with an x% format
       */
      public ProgressBar usingPercent() {
        this.textProgress = TextProgress.PERCENT_PROGRESS;
        steps = 100;
        return new ProgressBar(factory, this);
      }

      /**
       * Returns a {@code ProgressBar} that will display an x/y counter after the bar.
       * The {@code initialStepCount} can be updated later via
       * {@link ProgressBar#updateSteps updateSteps()}.
       *
       * <p>This method simply constructs the {@code ProgressBar}, nothing is written to the
       * console.
       *
       * @param initialStepCount the number of steps that will need to be taken to fill the bar
       * @return a progress bar with an x/y format
       */
      public ProgressBar usingCounter(int initialStepCount) {
        textProgress = TextProgress.FRACTION_PROGRESS;
        this.steps = initialStepCount;
        return new ProgressBar(factory, this);
      }
    }
  }

  /**
   * Returns a builder to configure a custom {@link ProgressBar}. Most users can simply use
   * {@link #percentProgressBar()} or {@link #counterProgressBar(int)}, but more options are
   * available via the builder.
   *
   * <p>For example, the following code:
   *
   * <pre>{@code ProgressBar customProgressBar = ansiUtil.progressBarBuilder()
   *     .prefix("<")
   *     .suffix(">")
   *     .barCharacter('-')
   *     .units(" tasks")
   *     .usingCounter(10);}</pre>
   *
   * <p>Would result in a progress bar that looks like:
   *
   * <pre>{@code <------------------------                          > 5/10 tasks}</pre>
   *
   * @return a builder to construct {@link ProgressBar} instances.
   */
  public ProgressBar.Builder progressBarBuilder() {
    return new ProgressBar.Builder(factory);
  }

  /**
   * Returns a {@code ProgressBar} that will display a percentage. Users will generally call
   * {@link ProgressBar#updateProgress} to update the percentage to display, however they can also
   * use {@link ProgressBar#updateSteps} to let the {@code ProgressBar} convert the current step
   * into a percentage.
   *
   * <p>This method simply constructs the {@code ProgressBar}, nothing is written to the console.
   *
   * <p>Example:
   *
   * <pre>{@code [=============================                            ] 50%}</pre>
   *
   * @return a {@link ProgressBar} instance starting at 0 percent complete
   */
  public ProgressBar percentProgressBar() {
    return progressBarBuilder().usingPercent();
  }

  /**
   * Returns a {@code ProgressBar} that will display an x/y counter. The {@code initialStepCount}
   * is the number of steps that will need to be taken to fill the bar. It can be updated later via
   * {@link ProgressBar#updateSteps updateSteps()}.
   *
   * <p>This method simply constructs the {@code ProgressBar}, nothing is written to the console.
   *
   * <p>Example:
   *
   * <pre>{@code [===========================                           ] 50/100}</pre>
   *
   * @param initialStepCount The number of steps to fill the progress bar
   * @return a {@link ProgressBar} instance starting at 0 steps complete
   */
  public ProgressBar counterProgressBar(int initialStepCount) {
    return progressBarBuilder().usingCounter(initialStepCount);
  }

  /**
   * Returns a {@code ProgressBar} that will display an x/y counter. The {@code initialStepCount}
   * is the number of steps that will need to be taken to fill the bar. It can be updated later via
   * {@link ProgressBar#updateSteps updateSteps()}.
   *
   * <p>This method simply constructs the {@code ProgressBar}, nothing is written to the console.
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
    return progressBarBuilder().units(units).usingCounter(initialStepCount);
  }
}
