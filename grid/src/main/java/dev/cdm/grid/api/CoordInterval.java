/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.grid.api;

import dev.cdm.core.util.Format;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.cdm.array.NumericCompare.nearlyEquals;

/**
 * A Coordinate represented by an interval [start, end). The interval can be positive or negetive.
 */
public record CoordInterval(double start, double end) {

  /** The midpoint between start and end. */
  public double midpoint() {
    return (start() + end()) / 2;
  }

  /** Compare two intervals to within the given tolerence. */
  public boolean fuzzyEquals(CoordInterval other, double tol) {
    return nearlyEquals(start(), other.start(), tol) && nearlyEquals(end(), other.end(), tol);
  }

  public String toString() {
    return toString(3);
  }

  /** Show the interval with given decimal precision. */
  public String toString(int ndecimals) {
    return Format.d(start(), ndecimals) + "-" + Format.d(end(), ndecimals);
  }

  private static final String decimal = "[+-]?([0-9]+([.][0-9]*)?|[.][0-9]+)";
  private static final String patternS = String.format("^(%s)[\\\\-](%s)$", decimal, decimal);
  private static final Pattern pattern = Pattern.compile(patternS);

  /**
   * The inverse of toString(), or null if cant parse.
   * startValue + "-" + endValue
   */
  @Nullable
  public static CoordInterval parse(String source) {
    if (source == null) {
      return null;
    }
    source = source.trim();
    Matcher m = pattern.matcher(source);
    if (!m.matches()) {
      return null;
    }
    show(m);
    if (m.groupCount() < 5) {
      return null;
    }
    try {
      double start = Double.parseDouble(m.group(1));
      double end = Double.parseDouble(m.group(4));
      return new CoordInterval(start, end);
    } catch (Exception e) {
      return null;
    }
  }

  private static final boolean show = false;

  private static void show(Matcher m) {
    if (!show)
      return;
    System.out.printf("matches = %d%n", m.groupCount());
    for (int i = 0; i < m.groupCount(); i++) {
      System.out.printf(" %d == %s%n", i, m.group(i));
    }
  }
}
