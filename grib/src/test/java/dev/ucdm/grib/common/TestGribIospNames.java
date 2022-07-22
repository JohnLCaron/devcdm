package dev.ucdm.grib.common;

import dev.ucdm.core.api.CdmFile;
import dev.ucdm.core.api.CdmFiles;
import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.grib.inventory.CollectionUpdate;
import dev.ucdm.grib.collection.GribCollection;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.oldTestDir;

public class TestGribIospNames {
  String testfile = oldTestDir + "tds_index/NCEP/NDFD/SPC/NDFD_SPC_CONUS_CONDUIT.ncx4";

  @Test
  public void testCdmFile() throws IOException {
    String varNameOld = "TwoD/Convective_Hazard_Outlook_surface_24_Hour_Average";
    String varName = "TwoD/Convective_Hazard_Outlook_surface_Average";
    try (CdmFile nc = CdmFiles.open(testfile)) {
      assertThat(nc.findVariable(varName)).isNull();
      assertThat(nc.findVariable(varNameOld)).isNotNull();
    }
  }

  @Test
  public void testCollectionIndex() throws IOException {
    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    try (RandomAccessFile raf = new RandomAccessFile(testfile, "r")) {
      try (GribCollection gc = GribCollectionIndex.openGribCollectionFromRaf(
              raf, CollectionUpdate.test, config, errlog)) {
        assertThat(gc).isNotNull();
        GribIosp iosp = gc.makeIosp();
        assertThat(iosp).isNotNull();

        System.out.printf("%s %s collectionType == %s%n", gc.name, iosp.getClass().getName(), iosp.gtype);
        gc.datasets.forEach( d -> {
          System.out.printf("  %s%n", d.gctype);
          d.groups.forEach( g -> {
            System.out.printf("    %s%n", g.horizCoordSys.getId());
            g.variList.forEach( vi -> {
              System.out.printf("      %s == %s%n", vi.id(), iosp.makeVariableName(vi));
            });
          });
        });
      }
    }
  }

}
