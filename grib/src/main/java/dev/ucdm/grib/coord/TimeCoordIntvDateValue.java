package dev.ucdm.grib.coord;

import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.core.calendar.CalendarPeriod;

/** Time intervals represented by start and end CalendarDate. */
public record TimeCoordIntvDateValue(CalendarDate start, CalendarDate end) implements Comparable<TimeCoordIntvDateValue> {

  public TimeCoordIntvDateValue(CalendarPeriod period, CalendarDate end) {
    this(end.add(-1, period), end);
  }

  public TimeCoordIntvDateValue(CalendarDate start, CalendarPeriod period) {
    this(start, start.add(period));
  }

  // Calculate the offset in units of timeUnit from the given reference date?
  public TimeCoordIntvValue convertReferenceDate(CalendarDate refDate, CalendarPeriod timeUnit) {
    if (timeUnit == null) {
      throw new IllegalArgumentException("null time unit");
    }
    int startOffset = (int) start.since(refDate, timeUnit);
    int endOffset = (int) end.since(refDate, timeUnit);
    // int endOffset = timeUnit.getOffset(refDate, end);
    return new TimeCoordIntvValue(startOffset, endOffset);
  }

  public int compareTo(TimeCoordIntvDateValue that) { // first compare start, then end
    int c1 = start.compareTo(that.start);
    return (c1 == 0) ? end.compareTo(that.end) : c1;
  }

  @Override
  public String toString() {
    return String.format("(%s,%s)", start, end);
  }

}

