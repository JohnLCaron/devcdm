/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.core.api;

import org.junit.jupiter.api.Test;
import dev.cdm.array.Array;
import dev.cdm.array.Arrays;
import dev.cdm.array.Range;
import dev.cdm.array.Section;

import java.util.ArrayList;

import static com.google.common.truth.Truth.assertThat;

/** Test reading variable data */
public class TestReadSection {

  @Test
  public void testReadVariableSection() throws Exception {
    try (CdmFile cdmfile = CdmFiles.open(TestCdmFiles.coreLocalNetcdf3Dir + "testWrite.nc")) {
      Variable temp = cdmfile.findVariable("temperature");
      assertThat(temp).isNotNull();

      int[] origin = {3, 6};
      int[] shape = {12, 17};

      Variable tempSection = temp.section(new Section(origin, shape));

      // read array section
      Array<Number> Asection = (Array<Number>) tempSection.readArray();
      assertThat(Asection.getRank()).isEqualTo(2);
      assertThat(shape[0]).isEqualTo(Asection.getShape()[0]);
      assertThat(shape[1]).isEqualTo(Asection.getShape()[1]);

      // read entire array
      Array<Number> A = (Array<Number>) temp.readArray();
      assertThat(A.getRank()).isEqualTo(2);

      // compare
      Array<Number> Asection2 = Arrays.section(A, new Section(origin, shape));
      assertThat(Asection2.getRank()).isEqualTo(2);
      assertThat(shape[0]).isEqualTo(Asection2.getShape()[0]);
      assertThat(shape[1]).isEqualTo(Asection2.getShape()[1]);

      assertThat(Arrays.equalNumbers(Asection, Asection2)).isTrue();
    }
  }

  @Test
  public void testReadVariableSection2() throws Exception {
    try (CdmFile cdmfile = CdmFiles.open(TestCdmFiles.coreLocalNetcdf3Dir + "testWrite.nc")) {
      Variable temp = cdmfile.findVariable("temperature");
      assertThat(temp).isNotNull();

      ArrayList<Range> ranges = new ArrayList<>();
      Range r0 = new Range(3, 14);
      Range r1 = new Range(6, 22);
      ranges.add(r0);
      ranges.add(r1);

      Variable tempSection = temp.section(new Section(ranges));
      assertThat(tempSection.getRank()).isEqualTo(2);
      int[] vshape = tempSection.getShape();
      assertThat(r0.length()).isEqualTo(vshape[0]);
      assertThat(r1.length()).isEqualTo(vshape[1]);

      // read array section
      Array<Number> Asection = (Array<Number>) tempSection.readArray();
      assertThat(Asection.getRank()).isEqualTo(2);
      assertThat(r0.length()).isEqualTo(Asection.getShape()[0]);
      assertThat(r1.length()).isEqualTo(Asection.getShape()[1]);

      // read entire array
      Array<Number> A = (Array<Number>) temp.readArray();
      assertThat(A.getRank()).isEqualTo(2);

      // compare
      Array<Number> Asection2 = Arrays.section(A, new Section(ranges));
      assertThat(Asection2.getRank()).isEqualTo(2);
      assertThat(r0.length()).isEqualTo(Asection2.getShape()[0]);
      assertThat(r1.length()).isEqualTo(Asection2.getShape()[1]);

      assertThat(Arrays.equalNumbers(Asection, Asection2)).isTrue();
    }
  }
}
