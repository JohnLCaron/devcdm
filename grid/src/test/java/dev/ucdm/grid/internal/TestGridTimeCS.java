package dev.ucdm.grid.internal;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import dev.ucdm.array.Range;
import dev.ucdm.core.calendar.CalendarDate;
import dev.ucdm.core.calendar.CalendarDateUnit;
import dev.ucdm.core.calendar.CalendarPeriod;
import dev.ucdm.core.constants.AxisType;
import dev.ucdm.grid.api.*;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link GridTimeCS} with builders (not real data) */
public class TestGridTimeCS {

  @Test
  public void testSingleRuntime() {
    String unit = "days since 1990-02-01T12:12";
    int nruntimes = 1;
    GridAxisPoint.Builder<?> rbuilder = GridAxisPoint.builder().setAxisType(AxisType.RunTime).setName("runtime")
        .setUnits(unit).setRegular(nruntimes, 1, 1).setSpacing(GridAxisSpacing.regularPoint);
    GridAxisPoint runtime = rbuilder.build();

    int ntimes = 7;
    double[] values = new double[] {0, 5, 10, 20, 40, 80, 100};
    GridAxisPoint.Builder<?> builder = GridAxisPoint.builder().setAxisType(AxisType.TimeOffset).setName("time")
        .setUnits("hours").setNcoords(ntimes).setValues(values).setSpacing(GridAxisSpacing.irregularPoint);
    GridAxisPoint timeAxis = builder.build();

    GridTimeCS subject = GridTimeCS.createSingleOrOffset(runtime, timeAxis);
    CalendarDateUnit cdu = CalendarDateUnit.fromUdunitString(null, unit).orElseThrow();

    assertThat(subject.getType()).isEqualTo(GridTimeCoordinateSystem.Type.SingleRuntime);
    assertThat(subject.getRuntimeDateUnit()).isEqualTo(cdu);
    assertThat(subject.getBaseDate()).isEqualTo(cdu.getBaseDateTime());
    assertThat(subject.getNominalShape()).isEqualTo(ImmutableList.of(nruntimes, ntimes));
    assertThat(subject.getSubsetRanges()).isEqualTo(ImmutableList.of(new Range(nruntimes), new Range(ntimes)));

    for (int runidx = 0; runidx < nruntimes; runidx++) {
      GridAxis<?> offset = subject.getTimeOffsetAxis(runidx);
      assertThat(offset.isInterval()).isFalse();
      assertThat(offset.isRegular()).isFalse();
      assertThat(offset.getNominalSize()).isEqualTo(ntimes);
      assertThat(offset.getAxisType()).isEqualTo(AxisType.TimeOffset);
      assertThat(offset.getDependenceType()).isEqualTo(GridAxisDependenceType.independent);
      assertThat(offset.getDependsOn()).isEqualTo(new ArrayList<>());
      assertThat(offset.getUnits()).isEqualTo("hours");
      assertThat(subject.getOffsetPeriod()).isEqualTo(CalendarPeriod.of("hours"));
      assertThat((Object) offset).isEqualTo(timeAxis);
    }

    assertThat((Object) subject.getRunTimeAxis()).isEqualTo(runtime);
    for (int runidx = 0; runidx < nruntimes; runidx++) {
      assertThat(subject.getRuntimeDate(runidx)).isEqualTo(cdu.makeCalendarDate(runidx + 1)); // start = 1, incr = 1
    }

    for (int runidx = 0; runidx < nruntimes; runidx++) {
      GridAxis<?> offset = subject.getTimeOffsetAxis(runidx);
      CalendarDate baseForRun = subject.getRuntimeDate(runidx);
      assertThat(baseForRun).isNotNull();
      List<CalendarDate> times = subject.getTimesForRuntime(runidx);
      assertThat(times).hasSize(ntimes);
      int offsetIdx = 0;
      for (CalendarDate time : times) {
        long what = (long) offset.getCoordDouble(offsetIdx);
        CalendarDate expected = baseForRun.add(what, subject.getOffsetPeriod());
        assertThat(time).isEqualTo(expected);
        offsetIdx++;
      }
    }
  }

  @Test
  public void testOffset() {
    String unit = "days since 1990-02-01T12:12";
    int nruntimes = 28;
    GridAxisPoint.Builder<?> rbuilder = GridAxisPoint.builder().setAxisType(AxisType.RunTime).setName("runtime")
        .setUnits(unit).setRegular(nruntimes, 1, 1).setSpacing(GridAxisSpacing.regularPoint);
    GridAxisPoint runtime = rbuilder.build();

    int ntimes = 7;
    double[] values = new double[] {0, 5, 10, 20, 40, 80, 100};
    GridAxisPoint.Builder<?> builder = GridAxisPoint.builder().setAxisType(AxisType.TimeOffset).setName("time")
        .setUnits("hours").setNcoords(ntimes).setValues(values).setSpacing(GridAxisSpacing.irregularPoint);
    GridAxisPoint timeAxis = builder.build();

    GridTimeCS subject = GridTimeCS.createSingleOrOffset(runtime, timeAxis);
    CalendarDateUnit cdu = CalendarDateUnit.fromUdunitString(null, unit).orElseThrow();

    assertThat(subject.getType()).isEqualTo(GridTimeCoordinateSystem.Type.Offset);
    assertThat(subject.getRuntimeDateUnit()).isEqualTo(cdu);
    assertThat(subject.getBaseDate()).isEqualTo(cdu.getBaseDateTime());
    assertThat(subject.getNominalShape()).isEqualTo(ImmutableList.of(nruntimes, ntimes));
    assertThat(subject.getSubsetRanges()).isEqualTo(ImmutableList.of(new Range(nruntimes), new Range(ntimes)));

    for (int runidx = 0; runidx < nruntimes; runidx++) {
      GridAxis<?> offset = subject.getTimeOffsetAxis(runidx);
      assertThat(offset.isInterval()).isFalse();
      assertThat(offset.isRegular()).isFalse();
      assertThat(offset.getNominalSize()).isEqualTo(ntimes);
      assertThat(offset.getAxisType()).isEqualTo(AxisType.TimeOffset);
      assertThat(offset.getDependenceType()).isEqualTo(GridAxisDependenceType.independent);
      assertThat(offset.getDependsOn()).isEqualTo(new ArrayList<>());
      assertThat(offset.getUnits()).isEqualTo("hours");
      assertThat(subject.getOffsetPeriod()).isEqualTo(CalendarPeriod.of("hours"));
      assertThat((Object) offset).isEqualTo(timeAxis);
    }

    assertThat((Object) subject.getRunTimeAxis()).isEqualTo(runtime);
    for (int runidx = 0; runidx < nruntimes; runidx++) {
      assertThat(subject.getRuntimeDate(runidx)).isEqualTo(cdu.makeCalendarDate(runidx + 1)); // start = 1, incr = 1
    }

    for (int runidx = 0; runidx < nruntimes; runidx++) {
      GridAxis<?> offset = subject.getTimeOffsetAxis(runidx);
      CalendarDate baseForRun = subject.getRuntimeDate(runidx);
      assertThat(baseForRun).isNotNull();
      List<CalendarDate> times = subject.getTimesForRuntime(runidx);
      assertThat(times).hasSize(ntimes);
      int offsetIdx = 0;
      for (CalendarDate time : times) {
        CalendarDate expected = baseForRun.add((long) offset.getCoordDouble(offsetIdx++), subject.getOffsetPeriod());
        assertThat(time).isEqualTo(expected);
      }
    }
  }

  @Test
  public void testObservation() {
    String unit = "days since 1990-02-01T12:12";
    int ntimes = 7;
    double[] values = new double[] {0, 5, 10, 20, 40, 80, 100};
    GridAxisPoint.Builder<?> builder = GridAxisPoint.builder().setAxisType(AxisType.TimeOffset).setName("time")
        .setUnits(unit).setNcoords(ntimes).setValues(values).setSpacing(GridAxisSpacing.irregularPoint);
    GridAxisPoint timeAxis = builder.build();

    GridTimeCS subject = GridTimeCS.createObservation(timeAxis);
    CalendarDateUnit cdu = CalendarDateUnit.fromUdunitString(null, unit).orElseThrow();

    assertThat(subject.getType()).isEqualTo(GridTimeCoordinateSystem.Type.Observation);
    assertThat(subject.getRuntimeDateUnit()).isEqualTo(cdu);
    assertThat(subject.getBaseDate()).isEqualTo(cdu.getBaseDateTime());
    assertThat(subject.getNominalShape()).isEqualTo(ImmutableList.of(ntimes));
    assertThat(subject.getSubsetRanges()).isEqualTo(ImmutableList.of(new Range(ntimes)));

    assertThat((Object) subject.getTimeOffsetAxis(0)).isEqualTo(timeAxis);
    assertThat(subject.getRuntimeDate(0)).isEqualTo(cdu.getBaseDateTime());
    assertThat(subject.getOffsetPeriod()).isEqualTo(CalendarPeriod.of("days"));

    List<CalendarDate> times = subject.getTimesForRuntime(0);
    assertThat(times).hasSize(ntimes);
    CalendarDate baseDate = subject.getBaseDate();
    for (int idx = 0; idx < ntimes; idx++) {
      CalendarDate expected = baseDate.add((long) timeAxis.getCoordDouble(idx), subject.getOffsetPeriod());
      assertThat(times.get(idx)).isEqualTo(expected);
    }
  }
}
