/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.coord;

import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.core.calendar.CalendarDateRange;
import dev.ucdm.core.calendar.CalendarPeriod;
import dev.ucdm.array.Immutable;

/**
 * Abstract superclass for time coordinates ( time, timeIntv, time2D)
 * Effectively Immutable
 */
@Immutable
public abstract class CoordinateTimeAbstract implements Coordinate {
  public static final String MIXED_INTERVALS = "Mixed_intervals";
  public static CalendarDateFactory cdf;

  final String periodName; // used to create the udunit
  protected final int code; // unit of time (Grib1 table 4, Grib2 table 4.4), eg hour, day, month
  protected final CalendarPeriod timeUnit; // time duration, based on code
  protected final CalendarDate refDate; // used to create the udunit
  protected final int[] time2runtime; // for each time, which runtime is used; index into master runtime

  protected String name = "time";

  CoordinateTimeAbstract(int code, CalendarPeriod timeUnit, CalendarDate refDate, int[] time2runtime) {
    this.code = code;
    this.timeUnit = timeUnit;
    this.refDate = (cdf == null) ? refDate : cdf.get(refDate);
    this.time2runtime = time2runtime;

    CalendarPeriod.Field cf = timeUnit.getField();
    if (cf == CalendarPeriod.Field.Month || cf == CalendarPeriod.Field.Year)
      this.periodName = "calendar " + cf;
    else
      this.periodName = cf.toString();
  }

  @Override
  public int getCode() {
    return code;
  }

  @Override
  public String getUnit() {
    return periodName;
  }

  public String getTimeUdUnit() {
    return periodName + " since " + refDate;
  }

  @Override
  public String getName() {
    return name;
  }

  public CoordinateTimeAbstract setName(String name) {
    if (!this.name.equals("time"))
      throw new IllegalStateException("Cant modify");
    this.name = name;
    return this;
  }

  public CalendarDate getRefDate() {
    return refDate;
  }

  public CalendarPeriod getTimeUnit() {
    return timeUnit;
  }

  public int[] getTime2runtime() {
    return time2runtime;
  }

  public int getMasterRuntimeIndex(int timeIdx) {
    if (time2runtime == null)
      return -1;
    if (timeIdx < 0 || timeIdx >= time2runtime.length)
      return -1;
    return time2runtime[timeIdx];
  }

  @Override
  public int getNCoords() {
    return getSize();
  }

  public double getOffsetInTimeUnits(CalendarDate start) {
    return refDate.since(start, timeUnit);
  }

  @Override
  public String toString() {
    return name;
  }

  public abstract CalendarDateRange makeCalendarDateRange();

}
