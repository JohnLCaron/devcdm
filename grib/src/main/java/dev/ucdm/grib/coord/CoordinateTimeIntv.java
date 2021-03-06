/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.coord;

import java.util.Arrays;

import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.core.calendar.CalendarDateRange;
import dev.ucdm.core.calendar.CalendarDateUnit;
import dev.ucdm.core.calendar.CalendarPeriod;
import dev.ucdm.array.Indent;
import dev.ucdm.core.util.Counters;
import dev.ucdm.grib.common.util.GribUtils;
import dev.ucdm.grib.grib1.record.Grib1ParamTime;
import dev.ucdm.grib.grib1.record.Grib1Record;
import dev.ucdm.grib.grib1.record.Grib1SectionProductDefinition;
import dev.ucdm.grib.grib1.table.Grib1Customizer;
import dev.ucdm.grib.grib2.record.Grib2Pds;
import dev.ucdm.grib.grib2.record.Grib2Record;
import dev.ucdm.grib.grib2.table.Grib2Tables;

import dev.ucdm.array.Immutable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

/** Time coordinates that are intervals. */
@Immutable
public class CoordinateTimeIntv extends CoordinateTimeAbstract implements Coordinate {
  private final List<TimeCoordIntvValue> timeIntervals;

  public CoordinateTimeIntv(int code, CalendarPeriod timeUnit, CalendarDate refDate,
      List<TimeCoordIntvValue> timeIntervals, int[] time2runtime) {
    super(code, timeUnit, refDate, time2runtime);
    this.timeIntervals = Collections.unmodifiableList(timeIntervals);
  }

  CoordinateTimeIntv(CoordinateTimeIntv org, CalendarDate refDate) {
    super(org.code, org.timeUnit, refDate, null);
    this.timeIntervals = org.getTimeIntervals();
  }

  public List<TimeCoordIntvValue> getTimeIntervals() {
    return timeIntervals;
  }

  @Override
  public List<?> getValues() {
    return timeIntervals;
  }

  @Override
  public Object getValue(int idx) {
    return timeIntervals.get(idx);
  }

  @Override
  public int getIndex(Object val) {
    return Collections.binarySearch(timeIntervals, (TimeCoordIntvValue) val);
  }

  @Override
  public int getSize() {
    return timeIntervals.size();
  }

  @Override
  public int estMemorySize() {
    return 616 + getSize() * (24); // LOOK probably wrong
  }

  @Override
  public Type getType() {
    return Type.timeIntv;
  }

  /**
   * Check if we all time intervals have the same length.
   * 
   * @return time interval name or MIXED_INTERVALS
   */
  public String getTimeIntervalName() {
    // are they the same length ?
    int firstValue = -1;
    for (TimeCoordIntvValue tinv : timeIntervals) {
      int value = (tinv.bounds2() - tinv.bounds1());
      if (firstValue < 0)
        firstValue = value;
      else if (value != firstValue)
        return MIXED_INTERVALS;
    }

    firstValue = (firstValue * timeUnit.getValue());
    return firstValue + "_" + timeUnit.getField().name();
  }

  /**
   * Make calendar date range, using the first and last ending bounds
   * 
   * @return calendar date range
   */
  @Override
  public CalendarDateRange makeCalendarDateRange() {
    CalendarDateUnit cdu = CalendarDateUnit.of(timeUnit, false, refDate);
    CalendarDate start = cdu.makeCalendarDate(timeUnit.getValue() * timeIntervals.get(0).bounds2());
    CalendarDate end = cdu.makeCalendarDate(timeUnit.getValue() * timeIntervals.get(getSize() - 1).bounds2());
    return CalendarDateRange.of(start, end);
  }

  @Override
  public void showInfo(Formatter info, Indent indent) {
    info.format("%s%s:", indent, getType());
    for (TimeCoordIntvValue cd : timeIntervals)
      info.format(" %s,", cd);
    info.format(" (%d) %n", timeIntervals.size());
    if (time2runtime != null)
      info.format("%stime2runtime: %s", indent, Arrays.toString(time2runtime));
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("Time Interval offsets: (%s) ref=%s%n", getTimeUnit(), getRefDate());
    for (TimeCoordIntvValue cd : timeIntervals)
      info.format("   (%3d - %3d)  %d%n", cd.bounds1(), cd.bounds2(), cd.bounds2() - cd.bounds1());
  }

  @Override
  public Counters calcDistributions() {
    Counters counters = new Counters();
    counters.add("resol");
    counters.add("intv");

    List<TimeCoordIntvValue> offsets = getTimeIntervals();
    for (int i = 0; i < offsets.size(); i++) {
      int intv = offsets.get(i).bounds2() - offsets.get(i).bounds1();
      counters.count("intv", intv);
      if (i < offsets.size() - 1) {
        int resol = offsets.get(i + 1).bounds1() - offsets.get(i).bounds1();
        counters.count("resol", resol);
      }
    }

    return counters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    CoordinateTimeIntv that = (CoordinateTimeIntv) o;

    if (code != that.code)
      return false;
    return timeIntervals.equals(that.timeIntervals);

  }

  @Override
  public int hashCode() {
    int result = code;
    result = 31 * result + timeIntervals.hashCode();
    return result;
  }

  ///////////////////////////////////////////////////////////

  public static class Builder2 extends CoordinateBuilderImpl<Grib2Record> {
    private final Grib2Tables cust;
    private final int code; // pdsFirst.getTimeUnit()
    private final CalendarPeriod timeUnit;
    private final CalendarDate refDate;

    public Builder2(Grib2Tables cust, int code, CalendarPeriod timeUnit, CalendarDate refDate) {
      this.cust = cust;
      this.code = code;
      this.timeUnit = timeUnit;
      this.refDate = refDate;
    }

    public Builder2(CoordinateTimeIntv from) {
      this.cust = null;
      this.code = from.getCode();
      this.timeUnit = from.getTimeUnit();
      this.refDate = from.getRefDate();
    }

    @Override
    public Object extract(Grib2Record gr) {
      TimeCoordIntvValue tinv;

      Grib2Pds pds = gr.getPDS();
      int tuInRecord = pds.getTimeUnit();
      if (tuInRecord == code) {
        int[] intv = cust.getForecastTimeIntervalOffset(gr);
        if (intv == null)
          throw new IllegalStateException("CoordinateTimeIntv must have TimeIntervalOffset");
        tinv = new TimeCoordIntvValue(intv[0], intv[1]);

      } else {
        TimeCoordIntvDateValue tinvd = cust.getForecastTimeInterval(gr); // converts to calendar date
        if (tinvd == null)
          throw new IllegalStateException("CoordinateTimeIntv has no ForecastTime");
        tinv = tinvd.convertReferenceDate(refDate, timeUnit);
        if (tinv == null)
          throw new IllegalStateException("CoordinateTimeIntv has no ReferenceTime");
      }

      return tinv;
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<TimeCoordIntvValue> offsetSorted = new ArrayList<>(values.size());
      for (Object val : values)
        offsetSorted.add((TimeCoordIntvValue) val);
      Collections.sort(offsetSorted);

      return new CoordinateTimeIntv(code, timeUnit, refDate, offsetSorted, null);
    }
  }

  public static class Builder1 extends CoordinateBuilderImpl<Grib1Record> {
    private final Grib1Customizer cust;
    private final int code; // pdsFirst.getTimeUnit()
    private final CalendarPeriod timeUnit;
    private final CalendarDate refDate;

    Builder1(Grib1Customizer cust, int code, CalendarPeriod timeUnit, CalendarDate refDate) {
      this.cust = cust;
      this.code = code;
      this.timeUnit = timeUnit;
      this.refDate = refDate;
    }

    @Override
    public Object extract(Grib1Record gr) {
      Grib1SectionProductDefinition pds = gr.getPDSsection();
      Grib1ParamTime ptime = gr.getParamTime(cust);
      int[] intv = ptime.getInterval();
      TimeCoordIntvValue tinv = new TimeCoordIntvValue(intv[0], intv[1]);

      // If its a different time unit, we have to adjust the interval.
      int tuInRecord = pds.getTimeUnit();
      if (tuInRecord != code) {
        CalendarPeriod unitInRecord = GribUtils.getCalendarPeriod(tuInRecord);
        tinv = tinv.convertReferenceDate(gr.getReferenceDate(), unitInRecord, refDate, timeUnit);
      }
      return tinv;
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<TimeCoordIntvValue> offsetSorted = new ArrayList<>(values.size());
      for (Object val : values)
        offsetSorted.add((TimeCoordIntvValue) val);
      Collections.sort(offsetSorted);

      return new CoordinateTimeIntv(code, timeUnit, refDate, offsetSorted, null);
    }
  }

}
