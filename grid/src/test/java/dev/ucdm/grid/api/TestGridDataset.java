/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grid.api;

import org.junit.jupiter.api.Test;
import dev.ucdm.core.api.Attribute;
import dev.ucdm.core.constants.AxisType;

import java.util.Formatter;
import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static dev.ucdm.test.util.TestFilesKt.extraTestDir;
import static dev.ucdm.test.util.TestFilesKt.oldTestDir;

/** Test {@link GridDataset} */
public class TestGridDataset {

  @Test
  public void testBasics() throws Exception {
    String filename = extraTestDir + "grid/GFS_Puerto_Rico_191km_20090729_0000.nc";
    Formatter errlog = new Formatter();

    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      System.out.println("readGridDataset: " + gds.getLocation());
      assertThat(gds.toString()).startsWith(String.format("name = GFS_Puerto_Rico_191km_20090729_0000.nc%n"));

      Grid grid = gds.findGridByAttribute("Grib_Variable_Id", "VAR_7-0-2-11_L100").orElseThrow();
      assertThat(grid.getName()).isEqualTo("Temperature_isobaric");
      Attribute att = grid.attributes().findAttribute("Grib_Variable_Id");
      assertThat(att).isEqualTo(new Attribute("Grib_Variable_Id", "VAR_7-0-2-11_L100"));

      assertThat(grid.toString()).startsWith("float Temperature_isobaric(time=20, isobaric1=6, y=39, x=45);");

      Optional<Grid> bad = gds.findGridByAttribute("failure", "VAR_7-0-2-11_L100");
      assertThat(bad).isEmpty();

      // test Grid
      assertThat(grid.getHorizCoordinateSystem()).isNotNull();
      assertThat(grid.getTimeCoordinateSystem()).isNotNull();

      // test GridCoordinateSystem
      assertThat(grid.getCoordinateSystem()).isNotNull();
      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat((Object) gcs.findCoordAxisByType(AxisType.Time)).isNotNull();
      assertThat((Object) gcs.findCoordAxisByType(AxisType.Ensemble)).isNull();

      assertThat(gcs.toString())
          .startsWith(String.format("Coordinate System (time isobaric1 y x)%n" + " time (GridAxisPoint) %n"
              + " isobaric1 (GridAxisPoint) %n" + " y (GridAxisPoint) %n" + " x (GridAxisPoint) "));

      assertThat(gcs.showFnSummary()).isEqualTo("GRID(T,Z,Y,X)");
    }
  }

  @Test
  public void testProblem() throws Exception {
    String filename = oldTestDir + "ft/grid/echoTops_runtime.nc";
    String gridName = "ECHO_TOP";
    Formatter errlog = new Formatter();

    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      System.out.println("readGridDataset: " + gds.getLocation());

      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      // test GridCoordinateSystem
      assertThat(grid.getCoordinateSystem()).isNotNull();
      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs.getNominalShape()).isEqualTo(List.of(24,1,1,1));

      GridReferencedArray data = grid.readData(GridSubset.create());
      assertThat(data).isNotNull();
      assertThat(data.materializedCoordinateSystem()).isNotNull();
      assertThat(data.materializedCoordinateSystem().getMaterializedShape()).isEqualTo(List.of(24,1,1,1));
    }
  }
}
