/*
 * Copyright (c) 1998-2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.api;

import dev.ucdm.array.Array;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.Arrays;
import dev.ucdm.array.CompareArrayToArray;
import dev.ucdm.array.StructureData;
import dev.ucdm.array.StructureDataArray;
import dev.ucdm.array.StructureMembers;

import static com.google.common.truth.Truth.assertThat;

/** Consistency checks for StructureDataArray */
public class CompareStructureArray {

  public void testArrayStructure(StructureDataArray as) {
    testArrayStructureByRecno(as);
    testArrayStructureByIterator(as);
    testArrayStructureByMemberArray(as);
  }

  private double testArrayStructureByMemberArray(StructureDataArray as) {

    for (StructureMembers.Member m : as.getStructureMembers()) {
      Array<?> memberArray = as.extractMemberArray(m);
      assertThat(memberArray.getArrayType()).isEqualTo(m.getArrayType());

      // random tests
      if (m.getArrayType() == ArrayType.STRING) {
        for (String s : (Array<String>) memberArray) {
          System.out.printf(" %s", s);
        }
      } else if (m.getArrayType() == ArrayType.CHAR) {
        System.out.printf(" %s", Arrays.makeStringFromChar((Array<Byte>) memberArray));
      } else if (m.getArrayType().isNumeric()) {
        return Arrays.sumDouble(memberArray);
      }
    }
    return 0.0;
  }

  private void testArrayStructureByIterator(StructureDataArray as) {
    for (StructureData sdata : as) {
      // run through each member on the StructureData
      for (StructureMembers.Member m : sdata.getStructureMembers()) {
        Array<?> sdataArray = sdata.getMemberData(m);
        assertThat(sdataArray.getArrayType()).isEqualTo(m.getArrayType());
      }
      testStructureData(sdata);
    }
  }

  private void testArrayStructureByRecno(StructureDataArray as) {
    // run through each StructureData
    for (int recno = 0; recno < as.getSize(); recno++) {
      StructureData sdata = as.get(recno);

      // run through each member on the StructureData
      for (StructureMembers.Member m : sdata.getStructureMembers()) {
        Array<?> sdataArray = sdata.getMemberData(m);
        assertThat(sdataArray.getArrayType()).isEqualTo(m.getArrayType());

        Array<?> sdataArray2 = sdata.getMemberData(m.getName());
        CompareArrayToArray.compareData(m.getName(), sdataArray, sdataArray2);

        Array<?> a = as.get(recno).getMemberData(m);
        CompareArrayToArray.compareData(m.getName(), sdataArray, a);

        testGetArrayByType(as, recno, m, a);
      }

      testStructureData(sdata);
    }
  }

  private void testGetArrayByType(StructureDataArray as, int recno, StructureMembers.Member m, Array<?> a) {
    assertThat(a.getArrayType()).isEqualTo(m.getArrayType());
    if (m.getArrayType() == ArrayType.STRUCTURE) {
      Array<?> nested = as.get(recno).getMemberData(m);
      testArrayStructure((StructureDataArray) nested);
    }
  }

  private void testStructureData(StructureData sdata) {
    for (StructureMembers.Member m : sdata.getStructureMembers()) {
      Array<?> sdataArray = sdata.getMemberData(m);
      assertThat(sdataArray.getArrayType()).isEqualTo(m.getArrayType());

      Array<?> sdataArray2 = sdata.getMemberData(m.getName());
      CompareArrayToArray.compareData(m.getName(), sdataArray, sdataArray2);
    }
  }

}
