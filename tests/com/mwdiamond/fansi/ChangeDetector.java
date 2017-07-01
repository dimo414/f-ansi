package com.mwdiamond.fansi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation indicating a test method is a change-detector, verifying behavior that may not be
 * part of the public API and therefore could change. When these methods need to be updated
 * increment the {@link #timesUpdated} property as a record of how often this occurs. A high update
 * count is a good indication that a test should be refactored or removed.
 */
@Target({ElementType.METHOD})
@interface ChangeDetector {
  int timesUpdated();
}
