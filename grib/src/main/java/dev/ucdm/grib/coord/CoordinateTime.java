/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.coord;

import java.util.Arrays;

import dev.ucdm.array.Indent;
import dev.ucdm.core.util.Counters;
import dev.ucdm.grib.common.util.GribUtils;
import dev.ucdm.grib.grib1.record.Grib1ParamTime;
import dev.ucdm.grib.grib1.record.Grib1Record;
import dev.ucdm.grib.grib1.record.Grib1SectionProductDefinition;
import dev.ucdm.grib.grib1.table.Grib1Customizer;
import dev.ucdm.grib.grib2.iosp.Grib2Utils;
import dev.ucdm.grib.grib2.record.Grib2Pds;
import dev.ucdm.grib.grib2.record.Grib2Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.core.calendar.CalendarDateRange;
import dev.ucdm.core.calendar.CalendarDateUnit;
import dev.ucdm.core.calendar.CalendarPeriod;
import dev.ucdm.array.Immutable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

/** Time coordinates that are offsets from the reference date (not intervals). */
@Immutable
public class CoordinateTime extends CoordinateTimeAbstract implements Coordinate {
  private static final Logger logger = LoggerFactory.getLogger(CoordinateTime.class);

  private final List<Long> offsetSorted;

  public CoordinateTime(int code, CalendarPeriod timeUnit, CalendarDate refDate, List<Long> offsetSorted,
      int[] time2runtime) {
    super(code, timeUnit, refDate, time2runtime);
    this.offsetSorted = Collections.unmodifiableList(offsetSorted);
  }

  CoordinateTime(CoordinateTime org, CalendarDate refDate) {
    super(org.code, org.timeUnit, refDate, null);
    this.offsetSorted = org.getOffsetSorted();
  }

  public List<Long> getOffsetSorted() {
    return offsetSorted;
  }

  @Override
  public List<?> getValues() {
    return offsetSorted;
  }

  @Override
  public int getIndex(Object val) {
    return Collections.binarySearch(offsetSorted, (Long) val);
  }

  @Override
  public Object getValue(int idx) {
    return offsetSorted.get(idx);
  }

  @Override
  public int getSize() {
    return offsetSorted.size();
  }

  @Override
  public Type getType() {
    return Type.time;
  }

  @Override
  public int estMemorySize() {
    return 320 + getSize() * (16);
  }

  @Override
  public CalendarDateRange makeCalendarDateRange() {
    CalendarDateUnit cdu = CalendarDateUnit.fromUdunitString(null, periodName + " since " + refDate).orElseThrow();
    CalendarDate start = cdu.makeCalendarDate(timeUnit.getValue() * offsetSorted.get(0));
    CalendarDate end = cdu.makeCalendarDate(timeUnit.getValue() * offsetSorted.get(getSize() - 1));
    return CalendarDateRange.of(start, end);
  }

  @Override
  public void showInfo(Formatter info, Indent indent) {
    info.format("%s%s:", indent, getType());
    for (Long cd : offsetSorted) {
      info.format(" %3d,", cd);
    }
    info.format(" (%d) %n", offsetSorted.size());
    if (time2runtime != null) {
      info.format("%stime2runtime: %s", indent, Arrays.toString(time2runtime));
    }
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("Time offsets: (%s) ref=%s %n", getTimeUnit(), getRefDate());
    for (Long cd : offsetSorted) {
      info.format("   %3d%n", cd);
    }
  }

  @Override
  public Counters calcDistributions() {
    Counters counters = new Counters();
    counters.add("resol");

    List<Long> offsets = getOffsetSorted();
    for (int i = 0; i < offsets.size() - 1; i++) {
      long diff = offsets.get(i + 1) - offsets.get(i);
      counters.count("resol", diff);
    }

    return counters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CoordinateTime that = (CoordinateTime) o;
    if (code != that.code) {
      return false;
    }
    return offsetSorted.equals(that.offsetSorted);
  }

  @Override
  public int hashCode() {
    int result = offsetSorted.hashCode();
    result = 31 * result + code;
    return result;
  }

  //////////////////////////////////////////////////////

  public static class Builder2 extends CoordinateBuilderImpl<Grib2Record> {
    private final int code; // pdsFirst.getTimeUnit()
    private final CalendarPeriod timeUnit;
    private final CalendarDate refDate;

    public Builder2(int code, CalendarPeriod timeUnit, CalendarDate refDate) {
      this.code = code;
      this.timeUnit = timeUnit;
      this.refDate = refDate;
    }

    public Builder2(CoordinateTime from) {
      this.code = from.getCode();
      this.timeUnit = from.getTimeUnit();
      this.refDate = from.getRefDate();
    }

    @Override
    public Object extract(Grib2Record gr) {
      Grib2Pds pds = gr.getPDS();
      int offset = pds.getForecastTime();
      int tuInRecord = pds.getTimeUnit();
      if (tuInRecord == code) {
        return (long) offset;
      } else {
        CalendarPeriod period = Grib2Utils.getCalendarPeriod(tuInRecord);
        if (period == null) {
          logger.warn("Cant find period for time unit=" + tuInRecord);
          return (long) offset;
        }
        CalendarDate validDate = refDate.add(offset, period);
        // timeUnit.getOffset(refDate, validDate);
        return validDate.since(refDate, timeUnit);
      }
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<Long> offsetSorted = new ArrayList<>(values.size());
      for (Object val : values) {
        offsetSorted.add((Long) val);
      }
      Collections.sort(offsetSorted);
      return new CoordinateTime(code, timeUnit, refDate, offsetSorted, null);
    }
  }

  public static class Builder1 extends CoordinateBuilderImpl<Grib1Record> {
    final Grib1Customizer cust;
    final int code; // pdsFirst.getTimeUnit()
    final CalendarPeriod timeUnit; // TODO could be a CalendarDateUnit
    final CalendarDate refDate;

    public Builder1(Grib1Customizer cust, int code, CalendarPeriod timeUnit, CalendarDate refDate) {
      this.cust = cust;
      this.code = code;
      this.timeUnit = timeUnit;
      this.refDate = refDate;
    }

    @Override
    public Object extract(Grib1Record gr) {
      Grib1SectionProductDefinition pds = gr.getPDSsection();
      Grib1ParamTime ptime = gr.getParamTime(cust);

      int offset = ptime.forecastTime();
      int tuInRecord = pds.getTimeUnit();
      if (tuInRecord == code) {
        return (long) offset;
      } else {
        CalendarDate validDate = GribUtils.getValidTime(refDate, tuInRecord, offset);
        // timeUnit.getOffset(refDate, validDate);
        return validDate.since(refDate, timeUnit);
      }
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<Long> offsetSorted = new ArrayList<>(values.size());
      for (Object val : values) {
        offsetSorted.add((Long) val);
      }
      Collections.sort(offsetSorted);
      return new CoordinateTime(code, timeUnit, refDate, offsetSorted, null);
    }
  }

}
