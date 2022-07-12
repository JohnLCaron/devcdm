/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.util;

/** Special Math functions. */
public final class SpecialMathFunction {
  private static final double log2 = Math.log(2);

  /**
   * Get the log base 2 of a number
   * 
   * @param x a double value
   * @return The log<sub>2</sub> of x
   * @throws ArithmeticException if (x < 0)
   */
  public static double log2(double x) throws ArithmeticException {
    if (x <= 0.0)
      throw new ArithmeticException("range exception");
    return Math.log(x) / log2;
  }

  /**
   * Get the atanh of a number.
   *
   * @param x a double value
   * @return The atanh of x
   * @throws ArithmeticException if x not in [-1, 1]
   */
  public static double atanh(double x) throws ArithmeticException {
    if ((x > 1.0) || (x < -1.0)) {
      throw new ArithmeticException("range exception");
    }
    return 0.5 * Math.log((1.0 + x) / (1.0 - x));
  }
}


