/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link dev.ucdm.core.api.CdmFiles} */
public class TestCdmFiles {
  public static final String coreLocalDir = "src/test/data/";
  public static final String coreLocalNetcdf3Dir = "src/test/data/netcdf3/";
  public static final String coreLocalNetcdf4Dir = "src/test/data/netcdf4/";

  @Test
  public void testOpenWithClassName() throws Exception {
    try (CdmFile ncfile = CdmFiles.open(coreLocalNetcdf3Dir + "longOffset.nc",
        "dev.cdm.core.netcdf3.N3iosp", -1, null, CdmFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      System.out.printf("%s%n", ncfile);
    }
  }

  @Test
  public void testOpenInMemory() throws IOException {
    try (CdmFile ncfile = CdmFiles.openInMemory(coreLocalNetcdf3Dir + "longOffset.nc")) {
      System.out.printf("%s%n", ncfile);
    }
  }

  @Test
  public void testCanOpen() {
    assertThat(CdmFiles.canOpen(coreLocalNetcdf3Dir + "longOffset.nc")).isTrue();
    assertThat(CdmFiles.canOpen(coreLocalNetcdf3Dir + "sunya.nc")).isFalse();
    assertThat(CdmFiles.canOpen(coreLocalNetcdf3Dir + "testUnsignedFillValueNew.dump")).isFalse();
  }

  @Test
  public void testMakeFullNameGroup() {
    // root
    // parent
    // child
    // grandchild
    Group.Builder parent = Group.builder().setName("parent");
    Group.Builder child = Group.builder().setName("child");
    parent.addGroup(child);
    Group.Builder grandchild = Group.builder().setName("grandchild");
    child.addGroup(grandchild);

    Group root = Group.builder().addGroup(parent).build();

    assertThat(CdmFiles.makeFullName(root)).isEqualTo("");

    assertThat(root.getGroups()).hasSize(1);
    Group parentGroup = root.getGroups().get(0);
    assertThat(CdmFiles.makeFullName(parentGroup)).isEqualTo("parent");

    assertThat(parentGroup.getGroups()).hasSize(1);
    Group childGroup = parentGroup.getGroups().get(0);
    assertThat(CdmFiles.makeFullName(childGroup)).isEqualTo("parent/child");

    assertThat(childGroup.getGroups()).hasSize(1);
    Group grandchildGroup = childGroup.getGroups().get(0);
    assertThat(CdmFiles.makeFullName(grandchildGroup)).isEqualTo("parent/child/grandchild");

  }
}
