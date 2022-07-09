package dev.ucdm.grib.common;

import dev.ucdm.grib.collection.MFile;
import dev.ucdm.grib.collection.MFileOS;
import dev.ucdm.grib.grib2.record.Grib2Gds;
import dev.ucdm.grib.grib2.record.Grib2Pds;
import dev.ucdm.grib.grib2.record.Grib2Record;
import dev.ucdm.grib.protoconvert.Grib2Index;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class TestGribIndex {

  private static Grib2Record record;

  @BeforeAll
  public static void createNewIndex() throws IOException {
    String testfile = "/home/snake/tmp/rugley.pds15.grib2";

    MFile mfile = MFileOS.getExistingFile(testfile);
    assertThat(mfile).isNotNull();

    Grib2Index gi = GribIndex.readOrCreateIndex(mfile, CollectionUpdateType.always, new Formatter());

    assertThat(gi).isNotNull();
    List<Grib2Record> records = gi.getRecords();
    record = records.get(0);
  }

  @Test
  public void checkPdsBasic() {
    Grib2Pds pds = record.getPDS();
    assertThat(pds.getRawLength()).isEqualTo(37);
    assertThat(pds.getTemplateNumber()).isEqualTo(15);
    assertThat(pds.getGenProcessType()).isEqualTo(2);
    assertThat(pds.getForecastTime()).isEqualTo(6);
  }

  @Test
  public void checkGdsBasic() {
    Grib2Gds gds = record.getGDS();
    assertThat(gds.isLatLon()).isEqualTo(true);
    assertThat(gds.template).isEqualTo(0);
    assertThat(gds.getNx()).isEqualTo(288);
    assertThat(gds.getNy()).isEqualTo(145);
  }
}
