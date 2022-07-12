/*
 * Copyright (c) 1998-2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.api;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import dev.ucdm.array.Array;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.Range;
import dev.ucdm.array.Section;
import dev.ucdm.array.StructureData;
import dev.ucdm.array.StructureMembers;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Test reading record data */
public class TestStructureReading {

  static CdmFile ncfile;

  @BeforeAll
  public static void setUp() throws Exception {
    // testWriteRecord is 1 dimensional (nc2 record dimension)
    ncfile = CdmFiles.open(TestCdmFiles.coreLocalNetcdf3Dir + "testWriteRecord.nc", -1, null,
        CdmFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
    System.out.printf("TestStructure %s%n", ncfile.getLocation());
  }

  @AfterAll
  public static void tearDown() throws Exception {
    ncfile.close();
  }

  @Test
  public void testNames() {
    List<Variable> vars = ncfile.getVariables();
    String[] trueNames = {"rh", "T", "lat", "lon", "time", "recordvarTest", "record"};
    for (int i = 0; i < vars.size(); i++) {
      assertThat(trueNames[i]).isEqualTo(vars.get(i).getFullName());
    }

    Structure record = (Structure) ncfile.findVariable("record");
    assertThat(record).isNotNull();

    vars = record.getVariables();
    String[] trueRecordNames = {"record.rh", "record.T", "record.time", "record.recordvarTest"};
    for (int i = 0; i < vars.size(); i++) {
      assertThat(trueRecordNames[i]).isEqualTo(vars.get(i).getFullName());
    }

    Variable time = ncfile.findVariable("record.time");
    assertThat(time).isNotNull();

    Variable time2 = record.findVariable("time");
    assertThat(time2).isNotNull();

    assertThat(time).isEqualTo(time2);
  }

  @Test
  public void testReadStructureCountBytesRead() throws Exception {
    Structure record = (Structure) ncfile.findVariable("record");
    assertThat(record).isNotNull();
    assertThat(record.getArrayType()).isEqualTo(ArrayType.STRUCTURE);
    Array<StructureData> data = (Array<StructureData>) record.readArray();

    // read all at once
    long totalAll = 0;
    for (dev.ucdm.array.StructureData sd : data) {
      for (StructureMembers.Member m : sd.getStructureMembers()) {
        Array<?> mdata = sd.getMemberData(m);
        totalAll += m.getStorageSizeBytes();
      }
    }
    assertThat(totalAll).isEqualTo(304);

    // read one at a time
    int numrecs = record.getShape()[0];
    long totalOne = 0;
    for (int i = 0; i < numrecs; i++) {
      Array<?> arr = record.readArray(Section.builder().appendRange(new Range(i, 1)).build());
      StructureData sd = (StructureData) arr.get(0);

      for (StructureMembers.Member m : sd.getStructureMembers()) {
        Array<?> mdata = sd.getMemberData(m);
        totalOne += m.getStorageSizeBytes();
      }
    }
    assertThat(totalOne).isEqualTo(totalAll);
  }

  @Test
  public void testN3ReadStructureCheckValues() throws Exception {
    Structure record = (Structure) ncfile.findVariable("record");
    assertThat(record).isNotNull();
    assertThat(record.getArrayType()).isEqualTo(ArrayType.STRUCTURE);
    Array<StructureData> data = (Array<StructureData>) record.readArray();

    // read all at once
    int recnum = 0;
    for (dev.ucdm.array.StructureData sd : data) {
      Array rh = sd.getMemberData("rh");
      checkValues(rh, recnum++); // check the values are right
    }

    // read one at a time
    recnum = 0;
    int numrecs = record.getShape()[0];
    for (int i = 0; i < numrecs; i++) {
      Array<?> arr = record.readArray(Section.builder().appendRange(new Range(i, 1)).build());
      StructureData sd = (StructureData) arr.get(0);
      Array rh = sd.getMemberData("rh");
      checkValues(rh, recnum++); // check the values are right
    }
  }

  private void checkValues(Array<?> rh, int recnum) {
    // check the values are right
    Array<Integer> rha = (Array<Integer>) rh;
    int[] shape = rha.getShape();
    for (int j = 0; j < shape[0]; j++) {
      for (int k = 0; k < shape[1]; k++) {
        int want = 20 * recnum + 4 * j + k + 1;
        int val = rha.get(j, k);
        assertThat(val).isEqualTo(want);
      }
    }
  }
}
