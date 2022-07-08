/* Copyright Unidata */
package dev.ucdm.grib.protoconvert;

import dev.ucdm.grib.common.CollectionUpdateType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test can read both proto2 and proto3 gbx9.
 * Also test failure mode for invalid proto.
 */
@RunWith(Parameterized.class)
public class TestGribIndexRead {
  private static final String gridTestDir = "../grib/src/test/data/index/";

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();
//    result.add(new Object[] {gridTestDir + "grib1.proto2.gbx9", true, false});
//    result.add(new Object[] {gridTestDir + "grib1.proto3.gbx9", true, true}); // fails
//    result.add(new Object[] {gridTestDir + "grib1.proto3.syntax2.gbx9", false, true}); // fails : grib1
    result.add(new Object[] {gridTestDir + "grib2.proto2.gbx9", false, false});
    result.add(new Object[] {gridTestDir + "grib2.proto3.gbx9", false, false}); // fails
    result.add(new Object[] {gridTestDir + "grib2.proto3.syntax2.gbx9", false, false});
    result.add(new Object[] {gridTestDir + "bad.gbx9", false, true}); // fails : doesnt exist
    result.add(new Object[] {gridTestDir + "../grib/src/test/data/afwa.grib1", false, true}); // fails : not gbx
    result.add(new Object[] {gridTestDir + "grib1.proto3.syntax2.gbx9", false, true}); // fails : grib1
    return result;
  }

  String filename;
  boolean isGrib1;
  boolean fail;

  public TestGribIndexRead(String filename, boolean isGrib1, boolean fail) {
    this.filename = filename;
    this.isGrib1 = isGrib1;
    this.fail = fail;
  }

  @Test
  public void testOpen() {
    /* if (isGrib1) {
      Grib1Index reader = new Grib1Index();
      boolean ok = reader.readIndex(filename, -1, CollectionUpdateType.never);
      assertThat(ok || fail).isTrue();
    } else {

     */
      Grib2Index index = Grib2RecordConvert.readGrib2Index(filename, -1, CollectionUpdateType.never);
      assertThat(index == null).isEqualTo(fail);
    // }
  }
}

