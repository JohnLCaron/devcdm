/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.api;

import com.google.common.collect.ImmutableList;
import dev.ucdm.core.api.AttributeContainerMutable;
import org.junit.jupiter.api.Test;
import dev.ucdm.array.ArrayType;
import dev.ucdm.core.constants.AxisType;
import dev.ucdm.core.constants.CDM;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.dataset.api.TestUtils.makeDummyGroup;

/** Test {@link CoordinateSystem.Builder} */
public class TestCoordSystemBuilder {

  @Test
  public void testBasics() {
    CdmDataset ncd = CdmDataset.builder().build();
    ArrayList<CoordinateAxis> axes = new ArrayList<>();

    VariableDS.Builder<?> xBuilder = VariableDS.builder().setName("xname").setArrayType(ArrayType.FLOAT)
        .setUnits("xunits").setDesc("xdesc").setEnhanceMode(CdmDataset.getEnhanceAll());
    axes.add(CoordinateAxis.fromVariableDS(xBuilder).setAxisType(AxisType.GeoX).build(makeDummyGroup()));

    VariableDS.Builder<?> yBuilder = VariableDS.builder().setName("yname").setArrayType(ArrayType.FLOAT)
        .setUnits("yunits").setDesc("ydesc").setEnhanceMode(CdmDataset.getEnhanceAll());
    axes.add(CoordinateAxis.fromVariableDS(yBuilder).setAxisType(AxisType.GeoY).build(makeDummyGroup()));

    CoordinateTransform projct = new CoordinateTransform(CDM.FlatEarth, AttributeContainerMutable.of(), true);
    List<CoordinateTransform> allProjs = ImmutableList.of(projct);

    CoordinateSystem.Builder<?> builder =
        CoordinateSystem.builder("xname yname").setCoordAxesNames("xname yname");
    builder.addTransformName(CDM.FlatEarth);
    CoordinateSystem coordSys = builder.build(axes, allProjs);

    CoordinateAxis xaxis = coordSys.findAxis(AxisType.GeoX);
    assertThat(xaxis).isNotNull();
    assertThat(xaxis.getShortName()).isEqualTo("xname");
    assertThat(xaxis.getArrayType()).isEqualTo(ArrayType.FLOAT);
    assertThat(xaxis.getUnitsString()).isEqualTo("xunits");
    assertThat(xaxis.getDescription()).isEqualTo("xdesc");
    assertThat(xaxis.getEnhanceMode()).isEqualTo(CdmDataset.getEnhanceAll());
    assertThat(xaxis.findAttributeString(CDM.UNITS, "")).isEqualTo("xunits");
    assertThat(xaxis.findAttributeString(CDM.LONG_NAME, "")).isEqualTo("xdesc");

    assertThat(coordSys.getProjection()).isNotNull();
    assertThat(coordSys.getProjection().getName()).isEqualTo("FlatEarth");

    assertThat(coordSys.isGeoReferencing()).isTrue();
    assertThat(coordSys.isGeoXY()).isTrue();
    assertThat(coordSys.isLatLon()).isFalse();

    CoordinateSystem copy = coordSys.toBuilder().build(axes, allProjs);
    assertThat(copy.findAxis(AxisType.GeoX)).isEqualTo(coordSys.findAxis(AxisType.GeoX));
    assertThat(copy.findAxis(AxisType.GeoY)).isEqualTo(coordSys.findAxis(AxisType.GeoY));
    assertThat(copy).isEqualTo(coordSys);
    assertThat(copy.hashCode()).isEqualTo(coordSys.hashCode());
  }

  @Test
  public void testFindMethods() {
    CdmDataset ncd = CdmDataset.builder().build();
    ArrayList<CoordinateAxis> axes = new ArrayList<>();

    VariableDS.Builder<?> xBuilder = VariableDS.builder().setName("xname").setArrayType(ArrayType.FLOAT)
        .setUnits("xunits").setDesc("xdesc").setEnhanceMode(CdmDataset.getEnhanceAll());
    axes.add(CoordinateAxis.fromVariableDS(xBuilder).setAxisType(AxisType.GeoX).build(makeDummyGroup()));

    VariableDS.Builder<?> yBuilder = VariableDS.builder().setName("yname").setArrayType(ArrayType.FLOAT)
        .setUnits("yunits").setDesc("ydesc").setEnhanceMode(CdmDataset.getEnhanceAll());
    axes.add(CoordinateAxis.fromVariableDS(yBuilder).setAxisType(AxisType.GeoY).build(makeDummyGroup()));

    CoordinateSystem.Builder<?> builder =
        CoordinateSystem.builder("xname yname").setCoordAxesNames("xname yname").setProjectionName("horiz");
    CoordinateSystem coordSys = builder.build(axes, ImmutableList.of());

    CoordinateAxis xaxis = coordSys.findAxis(AxisType.GeoX);
    assertThat(xaxis).isNotNull();
    assertThat(coordSys.findAxis(AxisType.GeoZ, AxisType.GeoX, AxisType.GeoY)).isEqualTo(xaxis);

    CoordinateAxis yaxis = coordSys.findAxis(AxisType.GeoY);
    assertThat(yaxis).isNotNull();
    assertThat(coordSys.findAxis(AxisType.GeoZ, AxisType.GeoY, AxisType.GeoX)).isEqualTo(yaxis);
    assertThat(coordSys.findAxis(AxisType.GeoZ, AxisType.Pressure, AxisType.Height)).isNull();

    assertThat(coordSys.findAxis(AxisType.GeoZ, AxisType.Pressure, AxisType.Height)).isNull();

    assertThat(coordSys.getProjection()).isNull();
  }
}
