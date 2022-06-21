package dev.cdm.grid.internal;

import dev.cdm.core.api.Attribute;
import dev.cdm.core.constants.AxisType;
import dev.cdm.dataset.geoloc.Earth;
import dev.cdm.dataset.geoloc.LatLonProjection;
import dev.cdm.dataset.geoloc.ProjectionPoint;
import dev.cdm.dataset.geoloc.ProjectionRect;
import dev.cdm.grid.api.GridAxisPoint;
import dev.cdm.grid.api.GridAxisSpacing;
import dev.cdm.grid.api.GridHorizCoordinateSystem;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Formatter;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

/**
 * Unit test for {@link CylindricalCoord}
 */
public class TestCylindricalCoord {

  private static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of(36, -50, 10, 0, 100, new int[] {0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100}), // full cylinder does not cross the seam A.2
            Arguments.of(36, -120, 10, 170, 40, new int[] {170, 180, 190, 200}), // full cylinder does cross the seam A.2

            Arguments.of(7, 0, 10, 90, 190, new int[0]), // partial no cross, A.1
            Arguments.of(11, 0, 10, 90, 190, new int[] {90, 100}), // partial no cross, one piece A.3
            Arguments.of(5, 0, 10, 130, 330, new int[] {0, 10, 20, 30, 40}), // B
            Arguments.of(11, 0, 10, 90, 300, new int[] {90, 100, 360, 370, 380, 390}), // twp pieces D
            Arguments.of(6, 150, 10, 90, 70, new int[] {150, 160}), // C

            Arguments.of(10, 0, 10, 170, 40, new int[0]) // empty A.1
    );
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testSubsetLon(int npts, int start, int incr, int subStart, int subWidth, int[] expect) {
    GridHorizCoordinateSystem hcs = makeGridHorizCoordinateSystem(npts, start, incr);
    CylindricalCoord cc = new CylindricalCoord(hcs);

    Formatter errlog = new Formatter();
    ProjectionRect subset1 = new ProjectionRect(new ProjectionPoint(subStart, 0), subWidth, 100);
    Optional<GridAxisPoint> result = cc.subsetLon(subset1, 1, errlog);
    if (expect.length == 0) {
      assertThat(result).isEmpty();
      return;
    }
    assertThat(result).isPresent();
    GridAxisPoint subset = result.get();
    try {
      int count = 0;
      for (Number val : subset) {
        if (count >= expect.length) break;
        assertThat(val).isEqualTo(expect[count]);
        count++;
      }
      assertThat(count).isEqualTo(expect.length);
    } catch (Throwable t) {
      Formatter format = new Formatter();
      for (Number val : subset) {
        format.format("%s, ", val);
      }
      System.out.printf("failing subset = %s%n", format);
      throw t;
    }
  }

  private GridHorizCoordinateSystem makeGridHorizCoordinateSystem(int npts, int start, int incr) {
    double[] latValues = new double[]{0, 5, 10, 20, 40, 80, 100};
    GridAxisPoint.Builder<?> builder = GridAxisPoint.builder().setAxisType(AxisType.Lat).setName("lat").setUnits("degN")
            .setDescription("desc").setNcoords(latValues.length).setValues(latValues).setSpacing(GridAxisSpacing.irregularPoint)
            .addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint latAxis = builder.build();

    GridAxisPoint.Builder<?> xbuilder = GridAxisPoint.builder().setAxisType(AxisType.Lon).setName("lon")
            .setUnits("degE").setDescription("desc").setRegular(npts, start, incr).setSpacing(GridAxisSpacing.regularPoint);
    GridAxisPoint lonAxis = xbuilder.build();

    LatLonProjection project = new LatLonProjection(new Earth());
    return new GridHorizCoordinateSystem(lonAxis, latAxis, project);
  }

}
