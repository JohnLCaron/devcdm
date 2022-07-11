package dev.ucdm.grib.grid;

import dev.cdm.grid.api.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.truth.Truth.assertThat;

public class TestGridGribDataset {

  @Test
  public void testGrib1() throws IOException {
    String testfile = "src/test/data/thinGrid.grib1";
    String gridName = "Temperature_isobaric";
    testOpen(testfile, gridName, new int[] {1, 1}, new int[] {1}, new int[] {73, 73});
  }

  @Test
  public void testGrib2() throws IOException {
    String testfile = "/home/snake/tmp/rugley.pds15.grib2";
    String gridName = "Clear_air_turbulence_CAT_isobaric_Maximum";
    testOpen(testfile, gridName, new int[] {1, 1}, new int[] {1}, new int[] {145, 288});
  }

  public static void testOpen(String endpoint, String gridName, int[] expectedTimeShape, int[] otherCoordShape,
                        int[] expectedHcsShape) throws IOException {
    System.out.printf("Test Dataset %s%n", endpoint);

    Formatter errlog = new Formatter();
    try (GribGridDataset gds = GribGridDataset.open(endpoint, errlog)) {
      assertThat(gds).isNotNull();

      Grid grid = gds.findGrid(gridName).orElse(null);
      assertThat(grid).isNotNull();

      GridCoordinateSystem cs = grid.getCoordinateSystem();
      assertThat(cs).isNotNull();

      GridTimeCoordinateSystem tcs = cs.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();
      assertThat(tcs.getNominalShape())
              .isEqualTo(Arrays.stream(expectedTimeShape).boxed().collect(Collectors.toList()));

      GridHorizCoordinateSystem hcs = cs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();
      assertThat(hcs.getShape()).isEqualTo(Arrays.stream(expectedHcsShape).boxed().collect(Collectors.toList()));

      List<Integer> expectedShape =
              IntStream.concat(IntStream.concat(Arrays.stream(expectedTimeShape), Arrays.stream(otherCoordShape)),
                      Arrays.stream(expectedHcsShape)).boxed().collect(Collectors.toList());
      assertThat(cs.getNominalShape()).isEqualTo(expectedShape);
    }
  }
}
