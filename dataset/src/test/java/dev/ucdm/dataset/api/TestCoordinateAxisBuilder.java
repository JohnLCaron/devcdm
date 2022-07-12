/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.api;

import org.junit.jupiter.api.Test;
import dev.ucdm.array.ArrayType;
import dev.ucdm.core.constants.AxisType;
import dev.ucdm.core.constants.CDM;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.dataset.api.TestUtils.makeDummyGroup;

/** Test {@link CoordinateAxis.Builder} */
public class TestCoordinateAxisBuilder {

  @Test
  public void testFromVariableDS() {
    VariableDS.Builder<?> vdsBuilder = VariableDS.builder().setName("name").setArrayType(ArrayType.FLOAT)
        .setUnits("units").setDesc("desc").setEnhanceMode(CdmDataset.getEnhanceAll());
    CoordinateAxis.Builder<?> builder = CoordinateAxis.fromVariableDS(vdsBuilder).setAxisType(AxisType.GeoX);
    CoordinateAxis axis = builder.build(makeDummyGroup());

    assertThat(axis.getShortName()).isEqualTo("name");
    assertThat(axis.getArrayType()).isEqualTo(ArrayType.FLOAT);
    assertThat(axis.getUnitsString()).isEqualTo("units");
    assertThat(axis.getDescription()).isEqualTo("desc");
    assertThat(axis.getEnhanceMode()).isEqualTo(CdmDataset.getEnhanceAll());
    assertThat(axis.findAttributeString(CDM.UNITS, "")).isEqualTo("units");
    assertThat(axis.findAttributeString(CDM.LONG_NAME, "")).isEqualTo("desc");

    CoordinateAxis copy = axis.toBuilder().build(makeDummyGroup());
    assertThat(copy).isEqualTo(axis);
    assertThat(copy.hashCode()).isEqualTo(axis.hashCode());
  }

}
