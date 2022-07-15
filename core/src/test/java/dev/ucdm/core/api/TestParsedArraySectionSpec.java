/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.api;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import dev.ucdm.array.Range;
import dev.ucdm.array.Section;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link dev.ucdm.core.api.ParsedArraySectionSpec} */
public class TestParsedArraySectionSpec {
  @Test
  public void testVariableSection() throws Exception {
    try (CdmFile ncfile = CdmFiles.open(TestCdmFiles.coreLocalNetcdf3Dir + "testWrite.nc")) {
      Variable v = ncfile.findVariable("temperature");
      assertThat(v).isNotNull();

      ParsedArraySectionSpec spec = ParsedArraySectionSpec.parseVariableSection(ncfile, "temperature");
      System.out.printf("%s%n", spec);
      assertThat(spec.getSection()).isEqualTo(v.getSection());

      spec = ParsedArraySectionSpec.parseVariableSection(ncfile, "temperature(1,0:127:2)");
      System.out.printf("%s%n", spec);
      Section sect = new Section("1,0:127:2");
      assertThat(spec.getSection()).isEqualTo(sect);

      spec = ParsedArraySectionSpec.parseVariableSection(ncfile, "temperature(:,0:127:2)");
      System.out.printf("%s%n", spec);
      sect = new Section("0:63,0:127:2");
      assertThat(spec.getSection()).isEqualTo(sect);

      String s = ParsedArraySectionSpec.makeSectionSpecString(v,
          new Section(ImmutableList.of(new Range(1, 1), new Range(0, 127, 2))));
      assertThat(s).isEqualTo("temperature(1:1, 0:126:2)");
    }
  }

  @Test
  public void testGroupAndMembers() throws Exception {
    try (CdmFile ncfile = CdmFiles.open(TestCdmFiles.coreLocalNetcdf4Dir + "simple_nc4.nc4")) {
      Variable v = ncfile.findVariable("grp1/data");
      assertThat(v).isNotNull();

      ParsedArraySectionSpec spec = ParsedArraySectionSpec.parseVariableSection(ncfile, "grp1/data");
      System.out.printf("%s%n", spec);
      assertThat(spec.getSection()).isEqualTo(v.getSection());

      spec = ParsedArraySectionSpec.parseVariableSection(ncfile, "grp2/data.i1");
      System.out.printf("%s%n", spec);

      Variable s = ncfile.findVariable("grp2/data");
      assertThat(spec.getSection()).isEqualTo(s.getSection());

      v = ncfile.findVariable("grp2/data.i1");
      assertThat(spec.getChild().getSection()).isEqualTo(v.getSection());
    }
  }

  @Test
  public void testMakeFromVariable() throws Exception {
    try (CdmFile ncfile = CdmFiles.open(TestCdmFiles.coreLocalNetcdf4Dir + "simple_nc4.nc4")) {
      Variable v = ncfile.findVariable("grp1/data");
      assertThat(v).isNotNull();

      ParsedArraySectionSpec spec = ParsedArraySectionSpec.makeFromVariable(v, v.getSection().toString());
      assertThat(spec.getSection()).isEqualTo(v.getSection());
      System.out.printf("%s%n", spec.makeSectionSpecString());
      assertThat(spec.makeSectionSpecString()).isEqualTo("grp1/data(0:5, 0:11)");
    }
  }

}
