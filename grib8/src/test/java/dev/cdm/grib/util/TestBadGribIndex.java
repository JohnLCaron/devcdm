/* Copyright Unidata */
package dev.cdm.grib.util;

import dev.cdm.core.api.CdmFile;
import dev.cdm.core.api.CdmFiles;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.inventory.CollectionUpdateType;
import dev.cdm.grib.grib1.Grib1Index;
import dev.cdm.grib.grib2.Grib2Index;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/** everything in this directory gives "Bad GRIB2 record"... "No records found in files", plus indexes are GRIB1 */
@Category(NeedsCdmUnitTest.class)
@RunWith(Parameterized.class)
public class TestBadGribIndex {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();
    result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/ecmwf/exclude/bcs/BCS10090000100903001"});
    return result;
  }

  String filename;

  public TestBadGribIndex(String filename) {
    this.filename = filename;
  }

  @Test
  public void testOpenGrib() {
    try (CdmFile ncfile = CdmFiles.open(filename, null)) {
      fail();
    } catch (Throwable t) {
      // expected
    }
  }

  @Test
  public void testReadAsGrib1() {
    String indexFilename = filename + ".gbx9";
    Grib2Index reader = new Grib2Index();
    boolean ok = reader.readIndex(indexFilename, -1, CollectionUpdateType.never);
    assertThat(ok).isFalse();
  }

  @Test
  public void testReadAsGrib2() {
    String indexFilename = filename + ".gbx9";
    Grib1Index reader = new Grib1Index();
    boolean ok = reader.readIndex(indexFilename, -1, CollectionUpdateType.never);
    assertThat(ok).isFalse();
  }
}

