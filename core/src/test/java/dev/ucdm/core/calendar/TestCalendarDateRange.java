/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.core.calendar;

import org.junit.jupiter.api.Test;
import dev.ucdm.core.calendar.CalendarPeriod.Field;

import java.util.Date;

import static com.google.common.truth.Truth.assertThat;

public class TestCalendarDateRange {

  @Test
  public void testBasics() {
    CalendarDate start = CalendarDate.of(2020, 7, 17, 11, 11, 11);
    CalendarDateRange range = CalendarDateRange.of(start, 3600);

    CalendarDate end = start.add(1, Field.Hour);
    CalendarDateRange range2 = CalendarDateRange.of(start, end);

    assertThat(range2).isEqualTo(range);
    assertThat(range2.hashCode()).isEqualTo(range.hashCode());
    assertThat(range.getEnd()).isEqualTo(end);

    CalendarDate end3 = CalendarDate.of(2222, 7, 17, 11, 11, 11);
    CalendarDateRange range3 = CalendarDateRange.of(start, end3);
    CalendarDateRange union = range.extend(range3);
    assertThat(union.getStart()).isEqualTo(start);
    assertThat(union.getEnd()).isEqualTo(end3);
  }

  @Test
  public void testDate() {
    Date start = new Date(2020 - 1900, 6, 17, 11, 11, 11);
    CalendarDate mid = CalendarDate.of(2020, 7, 23, 11, 11, 11);
    Date end = new Date(2020 - 1900, 7, 17, 11, 11, 11);
    CalendarDate past = CalendarDate.of(2020, 11, 11, 11, 11, 11);

    CalendarDateRange range = CalendarDateRange.of(start, end);

    assertThat(range.includes(mid)).isTrue();
    assertThat(range.includes(past)).isFalse();
  }

  @Test
  public void testIntersect() {
    CalendarDate start1 = CalendarDate.of(2020, 7, 17, 11, 11, 11);
    CalendarDateRange range1 = CalendarDateRange.of(start1, 3600);

    CalendarDate start2 = start1.add(1, Field.Hour);
    CalendarDateRange range2 = CalendarDateRange.of(start2, 3600);

    assertThat(range1.intersects(range1)).isTrue();
    assertThat(range1.intersect(range1).getDurationInSecs()).isEqualTo(3600);
    assertThat(range1.intersects(range2)).isTrue();
    assertThat(range1.intersect(range2).getDurationInSecs()).isEqualTo(0);

    CalendarDate start3 = start2.add(2, Field.Second);
    CalendarDateRange range3 = CalendarDateRange.of(start3, 3600);
    assertThat(range3.intersects(range1)).isFalse();
    assertThat(range3.intersects(range2)).isTrue();
    assertThat(range3.intersect(range2).getDurationInSecs()).isEqualTo(3598);
  }
}
