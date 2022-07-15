/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.util;

import dev.ucdm.array.Indent;
import dev.ucdm.core.api.Attribute;
import dev.ucdm.core.api.CdmFile;
import dev.ucdm.core.api.CdmFiles;
import dev.ucdm.core.api.Dimension;
import dev.ucdm.core.api.Group;
import dev.ucdm.core.api.Variable;
import dev.ucdm.test.util.FileFilterSkipSuffixes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.coreLocalDir;
import static dev.ucdm.test.util.TestFilesKt.testFilesIn;

/**
 * Test {@link CdmFullNames}
 */
public class TestCdmFullNames {
  private static final boolean show = false;

  public static Stream<Arguments> params() {
    // return Stream.of(Arguments.of(coreLocalDir + "/hdf5/GATRO-SATMR_npp_d20020906_t0409572_e0410270_b19646_c20090720223122943227_devl_int.h5"));

    return testFilesIn(coreLocalDir)
            .addNameFilter(new FileFilterSkipSuffixes(".cdl .txt"))
            .withRecursion()
            .build();
    /*
    var test1 = testFilesIn(coreLocalDir + "hdf5/")
            .addNameFilter(new FileFilterSkipSuffixes(".cdl .txt"))
            .withRecursion()
            .build();

    var test2 = testFilesIn(coreLocalDir + "hdf4/")
            .addNameFilter(new FileFilterSkipSuffixes(".cdl .txt"))
            .withRecursion()
            .build();

    return Stream.concat(test1, test2);

     */
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testCdmFullNames(String filename) throws Exception {
    System.out.printf(" %s%n", filename);
    try (CdmFile ncfile = CdmFiles.open(filename)) {
      CdmFullNames full = new CdmFullNames(ncfile);
      testCdmFullNames(full, ncfile, ncfile.getRootGroup(), new Indent(2, 2));
    }
  }

  void testCdmFullNames(CdmFullNames full, CdmFile ncfile, Group group, Indent indent) {
    if (show) System.out.printf("%sattributes%n", indent);
    assertThat(full.makeFullName(group)).isEqualTo(group.getFullName());
    assertThat(full.makeFullName(group)).isEqualTo(CdmFiles.makeFullName(group));

    for (Attribute att : group.attributes()) {
      String fullName = full.makeFullName(group, null, att);
      if (show) System.out.printf("%s  %s%n", indent, fullName);
      assertThat(ncfile.findAttribute(fullName)).isEqualTo(att);
    }

    if (show) System.out.printf("%sdimensions%n", indent);
    for (Dimension d : group.getDimensions()) {
      String fullName = full.makeFullName(group, d);
      CdmFullNames.DimensionWithGroup dg = full.findDimension(fullName);
      assertThat(dg.group()).isEqualTo(group);
      assertThat(dg.dim()).isEqualTo(d);

      if (!d.makeFullName(group).equals(fullName)) {
        System.out.printf(" HEY old Dimension fullname = %s%n", d.makeFullName(group));
      }
      assertThat(ncfile.findDimension(fullName)).isEqualTo(dg.dim());
    }

    if (show) System.out.printf("%svariables%n", indent);
    for (Variable v : group.getVariables()) {
      if (show) System.out.printf("%s  %s%n", indent, v.getFullName());
      assertThat(full.makeFullName(v)).isEqualTo(v.getFullName());
      assertThat(full.makeFullName(v)).isEqualTo(CdmFiles.makeFullName(v));

      assertThat(ncfile.findVariable(v.getFullName())).isEqualTo(v);
      testCdmFullNames(full, ncfile, group, v, indent.incr());
      indent.decr();
    }

    if (show) System.out.printf("%sgroups%n", indent);
    for (Group g : group.getGroups()) {
      testCdmFullNames(full, ncfile, g, indent.incr());
      indent.decr();
    }
  }

  void testCdmFullNames(CdmFullNames full, CdmFile ncfile, Group group, Variable v, Indent indent) {
    if (show) System.out.printf("%sattributes%n", indent);
    for (Attribute att : v.attributes()) {
      String fullName = full.makeFullName(group, v, att);
      if (show) System.out.printf("%s  %s%n", indent, fullName);
      assertThat(ncfile.findAttribute(fullName)).isEqualTo(att);
    }
  }

}
