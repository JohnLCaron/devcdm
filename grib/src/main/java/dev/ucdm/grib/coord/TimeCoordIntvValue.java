package dev.ucdm.grib.coord;

import javax.annotation.Nonnull;
import dev.ucdm.array.Immutable;
import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.core.calendar.CalendarPeriod;

/** time interval coordinate */
public record TimeCoordIntvValue(int bounds1, int bounds2) implements Comparable<TimeCoordIntvValue> {

  public int getIntervalSize() {
    return Math.abs(bounds2 - bounds1);
  }

  public TimeCoordIntvValue convertReferenceDate(CalendarDate fromDate, CalendarPeriod fromUnit, CalendarDate toDate,
      CalendarPeriod toUnit) {
    CalendarDate start = fromDate.add(bounds1, fromUnit);
    CalendarDate end = fromDate.add(bounds2, fromUnit);
    int startOffset = (int) start.since(toDate, toUnit);
    // int startOffset = toUnit.getOffset(toDate, start);
    int endOffset = (int) end.since(toDate, toUnit);
    // int endOffset = toUnit.getOffset(toDate, end);
    return new TimeCoordIntvValue(startOffset, endOffset);
  }

  @Override
  public int compareTo(@Nonnull TimeCoordIntvValue o) {
    int c1 = bounds2 - o.bounds2;
    return (c1 == 0) ? bounds1 - o.bounds1 : c1;
  }

  @Override
  public String toString() {
    return String.format("(%d,%d)", bounds1, bounds2);
  }

  public TimeCoordIntvValue offset(double offset) {
    return new TimeCoordIntvValue((int) (offset + bounds1), (int) (offset + bounds2));
  }
}

