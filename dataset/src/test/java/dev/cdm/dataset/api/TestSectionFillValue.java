/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.api;

import com.google.common.collect.Lists;
import dev.cdm.core.iosp.IospUtils;
import dev.cdm.dataset.internal.EnhanceScaleMissingUnsigned;
import org.junit.jupiter.api.Test;
import dev.cdm.array.Array;
import dev.cdm.array.IndexFn;
import dev.cdm.array.Range;
import dev.cdm.array.Section;
import dev.cdm.core.api.CdmFile;
import dev.cdm.core.api.Variable;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test fill values when reading sections of a Variable.
 * from (WUB-664639) (Didier Earith)
 */
public class TestSectionFillValue {

  @Test
  public void testExplicitFillValue() throws Exception {
    String filename = TestCdmDatasets.coreLocalDir + "standardVar.nc";
    try (CdmDataset ncfile = CdmDatasets.openDataset(filename)) {
      VariableDS v = (VariableDS) ncfile.findVariable("t3");
      assertThat(v).isNotNull();
      EnhanceScaleMissingUnsigned proxy = v.scaleMissingUnsignedProxy();
      assertThat(proxy.hasFillValue()).isTrue();
      assertThat(v.findAttribute("_FillValue")).isNotNull();

      int rank = v.getRank();
      List<Range> ranges = new ArrayList<>();
      ranges.add(null);
      for (int i = 1; i < rank; i++) {
        ranges.add(new Range(0, 1));
      }

      VariableDS v_section = (VariableDS) v.section(new Section(ranges));
      assertThat(v_section.findAttribute("_FillValue")).isNotNull();
      System.out.println(v_section.findAttribute("_FillValue"));
      assertThat(v_section.scaleMissingUnsignedProxy().hasFillValue()).isTrue();
    }
  }

  @Test
  public void testImplicitFillValue() throws Exception {
    String filename = TestCdmDatasets.coreLocalDir + "testWriteFill.nc";
    List<String> varWithFill = Lists.newArrayList("temperature", "rtemperature");
    try (CdmFile ncfile = CdmDatasets.openFile(filename, null);
         CdmDataset ncd = CdmDatasets.openDataset(filename)) {

      for (Variable v : ncfile.getVariables()) {
        if (!v.getArrayType().isNumeric())
          continue;
        System.out.printf("testImplicitFillValue for %s type=%s%n", v.getShortName(), v.getArrayType());

        VariableDS ve = (VariableDS) ncd.findVariable(v.getFullName());
        assertThat(ve).isNotNull();
        if (varWithFill.contains(v.getShortName())) {
          assertThat(v.findAttribute("_FillValue")).isNotNull();
          assertThat(ve.scaleMissingUnsignedProxy().hasFillValue()).isTrue();
          Number fillValue = v.findAttribute("_FillValue").getNumericValue();

          Array<Number> data = (Array<Number>) v.readArray();
          Array<Number> dataE = (Array<Number>) ve.readArray();
          IndexFn idxf = IndexFn.builder(data.getShape()).build();

          for (int idx = 0; idx < data.getSize(); idx++) {
            double vald = data.get(idxf.odometer(idx)).doubleValue();
            double valde = dataE.get(idxf.odometer(idx)).doubleValue();
            if (ve.scaleMissingUnsignedProxy().isFillValue(vald)) {
              if (v.getArrayType().isFloatingPoint()) {
                assertThat(valde).isNaN();
              } else {
                assertThat(vald).isEqualTo(fillValue);
              }
            }
          }
        } else {
          assertThat(v.findAttribute("_FillValue")).isNull();
          assertThat(ve.scaleMissingUnsignedProxy().hasFillValue()).isTrue();
          Number fillValue = IospUtils.getFillValueDefault(v.getArrayType());
          assertThat(fillValue).isNotNull();

          Array<Number> data = (Array<Number>) v.readArray();
          Array<Number> dataE = (Array<Number>) ve.readArray();
          IndexFn idxf = IndexFn.builder(data.getShape()).build();

          for (int idx = 0; idx < data.getSize(); idx++) {
            double vald = data.get(idxf.odometer(idx)).doubleValue();
            double valde = dataE.get(idxf.odometer(idx)).doubleValue();

            if (fillValue.equals(vald)) {
              assertThat(ve.scaleMissingUnsignedProxy().isFillValue(vald)).isTrue();
            }

            if (ve.scaleMissingUnsignedProxy().isFillValue(vald)) {
              if (v.getArrayType().isFloatingPoint()) {
                assertThat(valde).isNaN();
              } else {
                assertThat(valde).isEqualTo(fillValue.doubleValue());
              }
            }
          }
        }


      }
    }
  }


}
