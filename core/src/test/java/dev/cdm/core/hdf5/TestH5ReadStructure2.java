/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.core.hdf5;

import org.junit.jupiter.api.Test;
import dev.cdm.array.Array;
import dev.cdm.array.ArrayType;
import dev.cdm.array.Arrays;
import dev.cdm.array.StructureData;
import dev.cdm.array.StructureMembers;
import dev.cdm.core.api.Dimension;
import dev.cdm.core.api.CdmFile;
import dev.cdm.core.api.CdmFiles;
import dev.cdm.core.api.Structure;
import dev.cdm.core.api.Variable;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test hdf5 structures.
 */
public class TestH5ReadStructure2 {

  String filename = "src/test/data/hdf5/compound_complex.h5";

  String[] b_name = new String[] {"A fight is a contract that takes two people to honor.",
      "A combative stance means that you've accepted the contract.", "In which case, you deserve what you get.",
      "  --  Professor Cheng Man-ch'ing"};
  String c_name = "Hello!";

  @Test
  public void testReadH5Structure() throws java.io.IOException {
    try (CdmFile ncfile = CdmFiles.open(filename)) {
      Variable dset = ncfile.findVariable("CompoundComplex");
      assertThat(dset).isNotNull();
      assertThat(dset.getArrayType()).isEqualTo(ArrayType.STRUCTURE);
      assertThat(dset.getRank()).isEqualTo(1);
      assertThat(dset.getSize()).isEqualTo(6);

      Dimension d = dset.getDimension(0);
      assertThat(d.getLength()).isEqualTo(6);

      Structure s = (Structure) dset;

      Array<StructureData> iter = (Array<StructureData>) s.readArray();
      int a_name = 0;
      for (StructureData sd : iter) {
        assertThat((Integer) sd.getMemberData("a_name").getScalar()).isEqualTo(a_name);
        a_name++;
        Array<Byte> carr = (Array<Byte>) sd.getMemberData("c_name");
        assertThat(Arrays.makeStringFromChar(carr)).isEqualTo(c_name);
        Array<String> results = (Array<String>) sd.getMemberData("b_name");
        assertThat(results.length()).isEqualTo(b_name.length);
        int count = 0;
        for (String r : results) {
          assertThat(r).isEqualTo(b_name[count++]);
        }

        for (StructureMembers.Member m : sd.getStructureMembers()) {
          sd.getMemberData(m);
        }
      }

    }
    System.out.println("*** testReadH5Structure ok");
  }

  @Test
  public void testH5StructureDS() throws java.io.IOException {
    try (CdmFile ncfile = CdmFiles.open(filename)) {
      Variable dset = ncfile.findVariable("CompoundComplex");
      assertThat(dset).isNotNull();
      assertThat(dset.getArrayType()).isEqualTo(ArrayType.STRUCTURE);
      assertThat(dset.getRank()).isEqualTo(1);
      assertThat(dset.getSize()).isEqualTo(6);

      Structure s = (Structure) dset;

      // read all with the iterator
      Array<StructureData> iter = (Array<StructureData>) s.readArray();
      int a_name = 0;
      for (StructureData sd : iter) {
        assertThat((Integer) sd.getMemberData("a_name").getScalar()).isEqualTo(a_name);
        a_name++;
        Array<Byte> carr = (Array<Byte>) sd.getMemberData("c_name");
        assertThat(Arrays.makeStringFromChar(carr)).isEqualTo(c_name);
        Array<String> results = (Array<String>) sd.getMemberData("b_name");
        assertThat(results.length()).isEqualTo(b_name.length);
        int count = 0;
        for (String r : results) {
          assertThat(r).isEqualTo(b_name[count++]);
        }

        for (StructureMembers.Member m : sd.getStructureMembers()) {
          sd.getMemberData(m);
        }
      }
    }
    System.out.println("*** testH5StructureDS ok");
  }

}
