package dev.ucdm.grid.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import dev.ucdm.dataset.api.CdmDatasetCS;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import dev.ucdm.core.constants.FeatureType;
import dev.ucdm.dataset.api.CdmDatasets;
import dev.ucdm.grid.api.*;

import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static dev.ucdm.test.util.TestFilesKt.datasetLocalDir;
import static dev.ucdm.test.util.TestFilesKt.extraTestDir;
import static dev.ucdm.test.util.TestFilesKt.oldTestDir;

/** Test {@link DatasetClassifier} */
public class TestDatasetClassifier {

  @Test
  public void testNamPolar() throws IOException {
    String filename = oldTestDir + "tds_index/NCEP/NAM/Polar_90km/NAM_Polar_90km_20201027_0000.grib2.ncx4";

    try (CdmDatasetCS ds = CdmDatasets.openDatasetWithCS(filename, true)) {
      Formatter errlog = new Formatter();
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertWithMessage(errlog.toString()).that(grido.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = grido.get();
      if (!Iterables.isEmpty(gridDataset.getGrids())) {
        DatasetClassifier dclassifier = new DatasetClassifier(ds, errlog);
        DatasetClassifier.CoordSysClassifier classifier =
            dclassifier.getCoordinateSystemsUsed().stream().findFirst().orElse(null);
        assertThat(classifier).isNotNull();
        assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);
      }
    }
  }

  @Test
  public void testNamPolarCollection() throws IOException {
    String filename = oldTestDir + "tds_index/NCEP/NAM/Polar_90km/NAM-Polar_90km.ncx4";

    try (CdmDatasetCS ds = CdmDatasets.openDatasetWithCS(filename, true)) {
      Formatter errlog = new Formatter();
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertWithMessage(errlog.toString()).that(grido.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = grido.get();
      if (!Iterables.isEmpty(gridDataset.getGrids())) {
        DatasetClassifier dclassifier = new DatasetClassifier(ds, errlog);
        DatasetClassifier.CoordSysClassifier classifier =
            dclassifier.getCoordinateSystemsUsed().stream().findFirst().orElse(null);
        assertThat(classifier).isNotNull();
        assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);
      }
    }
  }

  @Test
  public void testNoGrids() throws IOException {
    // No Grids found because unidentified ensemble, time axis.
    String filename = datasetLocalDir + "ncml/testNested.ncml";
    try (CdmDatasetCS ds = CdmDatasets.openDatasetWithCS(filename, true)) {
      Formatter errlog = new Formatter();
      DatasetClassifier classifier = new DatasetClassifier(ds, errlog);
      assertThat(classifier.getFeatureType()).isNull();
      //Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      //assertThat(grido.isPresent()).isFalse();
    }
  }

  @Test
  public void testScalarRuntime() throws IOException {
    // scalar runtime
    String filename = oldTestDir + "tds/ncep/GEFS_Global_1p0deg_Ensemble_20120215_0000.grib2";
    try (CdmDatasetCS ds = CdmDatasets.openDatasetWithCS(filename, true)) {
      Formatter errlog = new Formatter();
      DatasetClassifier classifier = new DatasetClassifier(ds, errlog);
      assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);

      Optional<GridNetcdfDataset> gdso = GridNetcdfDataset.create(ds, errlog);
      assertThat(gdso.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = gdso.get();
      assertThat(Iterables.isEmpty(gridDataset.getGrids())).isFalse();
      System.out.printf("testScalarRuntime %s%n", gridDataset.getLocation());

      Optional<Grid> grido =
          gridDataset.findGrid("Convective_available_potential_energy_pressure_difference_layer_ens");
      assertThat(grido.isPresent()).isTrue();
      Grid grid = grido.get();
      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();
      GridTimeCoordinateSystem tcs = gcs.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();

      // rt (scalar), t, ens, z, y, x
      assertThat(Iterables.size(gcs.getGridAxes())).isEqualTo(6);
      assertThat((Object) tcs.getRunTimeAxis()).isNotNull();
      assertThat(tcs.getRunTimeAxis().getDependenceType() == GridAxisDependenceType.scalar).isTrue();
      assertThat(tcs.getRunTimeAxis().getDependenceType()).isEqualTo(GridAxisDependenceType.scalar);
      assertThat((Object) gcs.getEnsembleAxis()).isNotNull();
      assertThat((Object) gcs.getVerticalAxis()).isNotNull();
      assertThat((Object) gcs.getYHorizAxis()).isNotNull();
      assertThat((Object) gcs.getXHorizAxis()).isNotNull();

      assertThat(gcs.getNominalShape()).isEqualTo(ImmutableList.of(1, 65, 21, 1, 181, 360));
    }
  }

  @Test
  public void testScalarVert() throws IOException {
    // scalar runtime
    String filename = extraTestDir + "coords/ukmo.nc";
    try (CdmDatasetCS ds = CdmDatasets.openDatasetWithCS(filename, true)) {
      Formatter errlog = new Formatter();
      DatasetClassifier classifier = new DatasetClassifier(ds, errlog);
      assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);

      Optional<GridNetcdfDataset> gdso = GridNetcdfDataset.create(ds, errlog);
      assertThat(gdso.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = gdso.get();
      assertThat(Iterables.isEmpty(gridDataset.getGrids())).isFalse();

      Optional<Grid> grido = gridDataset.findGrid("temperature_2m");
      assertThat(grido.isPresent()).isTrue();
      Grid grid = grido.get();
      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();
      GridTimeCoordinateSystem tcs = gcs.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();

      // rt, to, z (scaler), y, x
      assertThat(Iterables.size(gcs.getGridAxes())).isEqualTo(5);
      assertThat((Object) tcs.getRunTimeAxis()).isNotNull();
      assertThat(tcs.getRunTimeAxis().getDependenceType() == GridAxisDependenceType.scalar).isFalse();
      assertThat(tcs.getRunTimeAxis().getDependenceType()).isEqualTo(GridAxisDependenceType.independent);
      assertThat((Object) tcs.getTimeOffsetAxis(0)).isNotNull();
      assertThat((Object) gcs.getEnsembleAxis()).isNull();
      assertThat((Object) gcs.getVerticalAxis()).isNotNull();
      assertThat((Object) gcs.getVerticalAxis().getDependenceType()).isEqualTo(GridAxisDependenceType.scalar);
      assertThat((Object) gcs.getYHorizAxis()).isNotNull();
      assertThat((Object) gcs.getXHorizAxis()).isNotNull();

      assertThat(gcs.getNominalShape()).isEqualTo(ImmutableList.of(5, 10, 77, 97));
    }
  }

  @Test
  public void testFMRC() throws IOException {
    String filename = oldTestDir + "gribCollections/cfsr/pwat.gdas.199612.grb2";

    try (CdmDatasetCS ds = CdmDatasets.openDatasetWithCS(filename, true)) {
      Formatter errlog = new Formatter();
      DatasetClassifier classifier = new DatasetClassifier(ds, errlog);
      assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);

      Optional<GridNetcdfDataset> gdso = GridNetcdfDataset.create(ds, errlog);
      assertThat(gdso.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = gdso.get();
      assertThat(Iterables.isEmpty(gridDataset.getGrids())).isFalse();
      assertThat(gridDataset.getFeatureType()).isEqualTo(FeatureType.GRID);
      System.out.printf("testFMRC %s%n", gridDataset.getLocation());

      Optional<Grid> grido = gridDataset.findGrid("Precipitable_water_entire_atmosphere_single_layer");
      assertThat(grido.isPresent()).isTrue();
      Grid grid = grido.get();
      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();
      GridTimeCoordinateSystem tcs = gcs.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();

      // rt, to, t (depend), y, x
      assertThat(Iterables.size(gcs.getGridAxes())).isEqualTo(4);
      assertThat((Object) tcs.getRunTimeAxis()).isNotNull();
      assertThat((Object) tcs.getTimeOffsetAxis(0)).isNotNull();
      assertThat(tcs.getTimeOffsetAxis(0).getDependenceType()).isEqualTo(GridAxisDependenceType.independent);
      assertThat(tcs.getRunTimeAxis().getDependenceType()).isEqualTo(GridAxisDependenceType.dependent);
      assertThat((Object) gcs.getEnsembleAxis()).isNull();
      assertThat((Object) gcs.getVerticalAxis()).isNull();
      assertThat((Object) gcs.getYHorizAxis()).isNotNull();
      assertThat((Object) gcs.getXHorizAxis()).isNotNull();

      assertThat(gcs.getNominalShape()).isEqualTo(ImmutableList.of(868, 576, 1152));
    }
  }

  // @Test
  @Disabled("TODO not dealing with multiple groups; coverage ver6 looks wrong also (ok in ver5)")
  public void problemWithGroups() throws IOException {
    String filename = extraTestDir + "gribCollections/ecmwf/mad/MAD10090000100900001";

    try (CdmDatasetCS ds = CdmDatasets.openDatasetWithCS(filename, true)) {
      Formatter errlog = new Formatter();
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertThat(grido.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = grido.get();
      if (!Iterables.isEmpty(gridDataset.getGrids())) {
        DatasetClassifier dclassifier = new DatasetClassifier(ds, errlog);
        DatasetClassifier.CoordSysClassifier classifier =
            dclassifier.getCoordinateSystemsUsed().stream().findFirst().orElse(null);
        assertThat(classifier).isNotNull();
        assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);
      }
    }
  }

  /*
   * Non-orthogonal offsets: Differs for hour 0 vs 12:
   * time2D: time runtime=reftime nruns=16 ntimes=16 isOrthogonal=false isRegular=true
   * All time values= 90, 96, 102, 108, 114, 120, 126, 132, 138, 144, 150, 156, 162, 168, 174, 180, 186, 192, (n=18)
   * hour 0: time: 102, 108, 114, 120, 126, 132, 138, 144, 150, 156, 162, 168, 174, 180, 186, 192, (16)
   * hour 12: time: 90, 96, 102, 108, 114, 120, 126, 132, 138, 144, 150, 156, 162, 168, 174, 180, (16)
   */
  @Test
  public void testRegularTimeOffset() throws IOException {
    String filename = oldTestDir + "gribCollections/gdsHashChange/noaaport/NDFD-CONUS_noaaport.ncx4";

    try (CdmDatasetCS ds = CdmDatasets.openDatasetWithCS(filename, true)) {
      Formatter errlog = new Formatter();
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertThat(grido.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = grido.get();
      if (!Iterables.isEmpty(gridDataset.getGrids())) {
        DatasetClassifier dclassifier = new DatasetClassifier(ds, errlog);
        DatasetClassifier.CoordSysClassifier classifier =
            dclassifier.getCoordinateSystemsUsed().stream().findFirst().orElse(null);
        assertThat(classifier).isNotNull();
        assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);
      }
    }
  }

  @Test
  public void testMRUTP() throws IOException {
    String filename = oldTestDir + "tds_index/NCEP/GFS/Global_0p25deg_ana/GFS-Global_0p25deg_ana.ncx4";

    // MRUTP has a runtime and time sharing the same dimension.
    try (CdmDatasetCS ds = CdmDatasets.openDatasetWithCS(filename, true)) {
      Formatter errlog = new Formatter();
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertThat(grido.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = grido.get();
      if (!Iterables.isEmpty(gridDataset.getGrids())) {
        DatasetClassifier dclassifier = new DatasetClassifier(ds, errlog);
        DatasetClassifier.CoordSysClassifier classifier =
            dclassifier.getCoordinateSystemsUsed().stream().findFirst().orElse(null);
        assertThat(classifier).isNotNull();
        assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);
      }
    }
  }

  @Test
  public void testMRUTP2() throws IOException {
    String filename = oldTestDir + "gribCollections/anal/HRRRanalysis.ncx4";

    try (CdmDatasetCS ds = CdmDatasets.openDatasetWithCS(filename, true)) {
      Formatter errlog = new Formatter();
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertThat(grido.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = grido.get();
      if (!Iterables.isEmpty(gridDataset.getGrids())) {
        DatasetClassifier dclassifier = new DatasetClassifier(ds, errlog);
        DatasetClassifier.CoordSysClassifier classifier =
            dclassifier.getCoordinateSystemsUsed().stream().findFirst().orElse(null);
        assertThat(classifier).isNotNull();
        assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);
      }
    }
  }

  @Test
  public void testMRUTP3() throws IOException {
    String filename = oldTestDir + "gribCollections/rdavm/ds627.0/ei.oper.an.pv/ds627.0_46.ncx4";

    try (CdmDatasetCS ds = CdmDatasets.openDatasetWithCS(filename, true)) {
      Formatter errlog = new Formatter();
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertThat(grido.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = grido.get();
      if (!Iterables.isEmpty(gridDataset.getGrids())) {
        DatasetClassifier dclassifier = new DatasetClassifier(ds, errlog);
        DatasetClassifier.CoordSysClassifier classifier =
            dclassifier.getCoordinateSystemsUsed().stream().findFirst().orElse(null);
        assertThat(classifier).isNotNull();
        assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);
      }
    }
  }

  @Test
  public void testBothXYandLatLon() throws IOException {
    String filename = extraTestDir + "grid/testCFwriter.nc";
    String gridName = "Temperature";
    System.out.printf("file %s coverage %s%n", filename, gridName);

    try (CdmDatasetCS ds = CdmDatasets.openDatasetWithCS(filename, true)) {
      Formatter errlog = new Formatter();
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertThat(grido.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = grido.get();
      if (!Iterables.isEmpty(gridDataset.getGrids())) {
        DatasetClassifier dclassifier = new DatasetClassifier(ds, errlog);
        DatasetClassifier.CoordSysClassifier classifier =
            dclassifier.getCoordinateSystemsUsed().stream().findFirst().orElse(null);
        assertThat(classifier).isNotNull();
        assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);
      }
    }
  }

  @Test
  public void testCurvilinear1D() throws IOException {
    String filename = extraTestDir + "curvilinear/Run_20091025_0000.nc";
    String gridName = "temp";
    System.out.printf("file %s coverage %s%n", filename, gridName);

    try (CdmDatasetCS ds = CdmDatasets.openDatasetWithCS(filename, true)) {
      Formatter errlog = new Formatter();
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertThat(grido.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = grido.get();
      if (!Iterables.isEmpty(gridDataset.getGrids())) {
        DatasetClassifier dclassifier = new DatasetClassifier(ds, errlog);
        DatasetClassifier.CoordSysClassifier classifier =
            dclassifier.getCoordinateSystemsUsed().stream().findFirst().orElse(null);
        assertThat(classifier).isNotNull();
        assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.CURVILINEAR);
      }
    }
  }

  @Test
  public void testCurvilinear() throws IOException {
    String filename = extraTestDir + "curvilinear/mississippi.nc";
    String gridName = "temp";
    System.out.printf("file %s coverage %s%n", filename, gridName);

    try (CdmDatasetCS ds = CdmDatasets.openDatasetWithCS(filename, true)) {
      Formatter errlog = new Formatter();
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertThat(grido.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = grido.get();
      if (!Iterables.isEmpty(gridDataset.getGrids())) {
        DatasetClassifier dclassifier = new DatasetClassifier(ds, errlog);
        DatasetClassifier.CoordSysClassifier classifier =
            dclassifier.getCoordinateSystemsUsed().stream().findFirst().orElse(null);
        assertThat(classifier).isNotNull();
        assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.CURVILINEAR);
      }
    }
  }

  @Test
  public void testCurvilinear2() throws IOException {
    String filename = extraTestDir + "curvilinear/signell_bathy_fixed.nc";
    String gridName = "h";
    System.out.printf("file %s coverage %s%n", filename, gridName);

    try (CdmDatasetCS ds = CdmDatasets.openDatasetWithCS(filename, true)) {
      Formatter errlog = new Formatter();
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertThat(grido.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = grido.get();
      if (!Iterables.isEmpty(gridDataset.getGrids())) {
        DatasetClassifier dclassifier = new DatasetClassifier(ds, errlog);
        DatasetClassifier.CoordSysClassifier classifier =
            dclassifier.getCoordinateSystemsUsed().stream().findFirst().orElse(null);
        assertThat(classifier).isNotNull();
        assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.CURVILINEAR);
      }
    }
  }



}
