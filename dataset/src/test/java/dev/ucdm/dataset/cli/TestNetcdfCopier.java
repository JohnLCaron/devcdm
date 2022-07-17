package dev.ucdm.dataset.cli;

import dev.ucdm.core.api.CdmFile;
import dev.ucdm.core.write.Netcdf3FormatWriter;
import dev.ucdm.core.write.NetcdfCopier;
import dev.ucdm.dataset.api.CdmDatasets;
import dev.ucdm.test.util.CompareCdmDataset;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.Formatter;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.datasetLocalNcmlDir;
import static dev.ucdm.test.util.TestFilesKt.gribLocalDir;

public class TestNetcdfCopier {

  @TempDir
  public static File tempFolder;

  public static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of(datasetLocalNcmlDir + "nc/time0.nc"),
            Arguments.of(datasetLocalNcmlDir + "modifyVars.xml"),
            Arguments.of(datasetLocalNcmlDir + "testReadOverride.xml"),
            Arguments.of(gribLocalDir + "Lannion.pds31.grib2"),
            Arguments.of(gribLocalDir + "afwa.grib1")
    );
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testNetcdfCopier(String filein) throws Exception {
    String fileout = File.createTempFile("TestNetcdfCopier", ".nc", tempFolder).getAbsolutePath();
    Netcdf3FormatWriter.Builder<?> ncwriter = Netcdf3FormatWriter.createNewNetcdf3(fileout);

    try (CdmFile ncfile = CdmDatasets.openDataset(filein)) {

      try (NetcdfCopier copier = NetcdfCopier.create(ncfile, ncwriter)) {
        copier.write(null);
      }

      try (CdmFile copy = CdmDatasets.openDataset(fileout)) {
        Formatter out = new Formatter();
        var comparer = new CompareCdmDataset(out, true, true, true);
        boolean ok = comparer.compare(ncfile, copy);
        System.out.printf(" %s: compare ok = %s%n", filein, ok);
        if (!ok) {
          System.out.printf(" %s%n", out);
        }
      }
    }
  }
}
