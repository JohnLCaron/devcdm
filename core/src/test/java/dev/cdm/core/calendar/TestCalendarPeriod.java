package dev.cdm.core.calendar;

import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Test {@link CalendarPeriod} */
public class TestCalendarPeriod {

  @Test
  public void testFromUnitString() {
    CalendarPeriod.Field fld = CalendarPeriod.fromUnitString("days");
    assertThat(fld).isEqualTo(CalendarPeriod.Field.Day);

    CalendarPeriod period1 = CalendarPeriod.of("days");
    assertThat(period1.getField()).isEqualTo(CalendarPeriod.Field.Day);
    assertThat(period1.getValue()).isEqualTo(1);
    assertThat(period1.toString()).isEqualTo("1 days");

    CalendarPeriod period = CalendarPeriod.of("11 months");
    assertThat(period.getField()).isEqualTo(CalendarPeriod.Field.Month);
    assertThat(period.getValue()).isEqualTo(11);
    assertThat(period.toString()).isEqualTo("11 months");

    assertThat(CalendarPeriod.of(null)).isNull();
    assertThat(CalendarPeriod.of("")).isNull();
    assertThat(CalendarPeriod.of("11 months from now")).isNull();
    assertThat(CalendarPeriod.of("months from now")).isNull();
  }

  @Test
  public void testGet() {
    for (CalendarPeriod.Field field : CalendarPeriod.Field.values()) {
      assertThat(CalendarPeriod.fromUnitString(field.name())).isEqualTo(field);
    }

    assertThat(CalendarPeriod.fromUnitString("s")).isEqualTo(CalendarPeriod.Field.Second);
    assertThat(CalendarPeriod.fromUnitString("ms")).isEqualTo(CalendarPeriod.Field.Millisec);

    assertThat(CalendarPeriod.fromUnitString(null)).isNull();
    assertThat(CalendarPeriod.fromUnitString("bad")).isNull();
  }

  @Test
  public void testGetConvertFactor() {
    CalendarPeriod sec = CalendarPeriod.Second;

    assertThat(sec.getConvertFactor(CalendarPeriod.Millisec)).isEqualTo(.001);
    assertThat(sec.getConvertFactor(CalendarPeriod.Second)).isEqualTo(1);
    assertThat(sec.getConvertFactor(CalendarPeriod.Minute)).isEqualTo(60);
    assertThat(sec.getConvertFactor(CalendarPeriod.Hour)).isEqualTo(3600);
    assertThat(sec.getConvertFactor(CalendarPeriod.of(2, CalendarPeriod.Field.Day))).isEqualTo(2 * 3600 * 24);

    assertThrows(IllegalStateException.class, () -> sec.getConvertFactor(CalendarPeriod.of(12, CalendarPeriod.Field.Month)));
    assertThrows(IllegalStateException.class, () -> sec.getConvertFactor(CalendarPeriod.of(1, CalendarPeriod.Field.Year)));
  }

}
