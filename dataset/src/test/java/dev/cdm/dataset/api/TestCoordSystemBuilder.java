/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.api;

import com.google.common.collect.ImmutableList;
import dev.cdm.dataset.geoloc.projection.FlatEarth;
import dev.cdm.dataset.transform.horiz.ProjectionCTV;
import org.junit.jupiter.api.Test;
import dev.cdm.array.ArrayType;
import dev.cdm.core.constants.AxisType;
import dev.cdm.core.constants.CDM;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static dev.cdm.dataset.api.TestUtils.makeDummyGroup;

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

    ProjectionCTV projct = new ProjectionCTV("horiz", new FlatEarth());
    List<ProjectionCTV> allProjs = ImmutableList.of(projct);

    CoordinateSystem.Builder<?> builder =
        CoordinateSystem.builder().setCoordAxesNames("xname yname").setCoordinateTransformName("horiz");
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

    assertThat(coordSys.getProjection()).isEqualTo(projct.getPrecomputedProjection());

    assertThat(coordSys.isImplicit()).isFalse();
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
        CoordinateSystem.builder().setCoordAxesNames("xname yname").setCoordinateTransformName("horiz");
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
