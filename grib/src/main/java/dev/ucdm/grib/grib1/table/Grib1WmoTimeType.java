/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grib1.table;

import org.jetbrains.annotations.Nullable;
import dev.ucdm.grib.common.GribStatType;

/**
 * Standard WMO tables for time range indicator - Grib1 table 5.
 *
 * @author caron
 * @since 1/13/12
 */
public class Grib1WmoTimeType {

  /**
   * The time unit statistical type, derived from code table 5)
   *
   * @return time unit statistical type, or null if unknown.
   */
  @Nullable
  public static GribStatType getStatType(int timeRangeIndicator) {
    return switch (timeRangeIndicator) {
      case 3, 6, 7, 51, 113, 115, 117, 120, 123 -> GribStatType.Average;
      case 4, 114, 116, 124 -> GribStatType.Accumulation;
      case 5 -> GribStatType.DifferenceFromEnd;
      case 118 -> GribStatType.Covariance;
      case 119, 125 -> GribStatType.StandardDeviation;
      default -> null;
    };
  }

}
