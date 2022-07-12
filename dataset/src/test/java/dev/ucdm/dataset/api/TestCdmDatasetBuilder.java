/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.api;

import dev.ucdm.dataset.coordsysbuild.CoordsHelperBuilder;
import org.junit.jupiter.api.Test;
import dev.ucdm.array.ArrayType;
import dev.ucdm.core.api.Attribute;
import dev.ucdm.core.api.Dimension;
import dev.ucdm.core.api.Group;
import dev.ucdm.core.api.Variable;
import dev.ucdm.core.constants.AxisType;
import dev.ucdm.core.constants.CDM;

import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link CdmDataset.Builder} */
public class TestCdmDatasetBuilder {

  @Test
  public void testBasics() {
    Attribute att = new Attribute("attName", "value");
    Dimension dim = new Dimension("dimName", 42);
    Group.Builder nested = Group.builder().setName("child");
    VariableDS.Builder<?> vb = VariableDS.builder().setName("varName").setArrayType(ArrayType.STRING);
    Group.Builder groupb =
        Group.builder().setName("").addAttribute(att).addDimension(dim).addGroup(nested).addVariable(vb);
    nested.setParentGroup(groupb);

    CdmDataset.Builder<?> builder =
        CdmDataset.builder().setId("Hid").setLocation("location").setRootGroup(groupb).setTitle("title");

    CdmDataset ncfile = builder.build();
    assertThat(ncfile.getId()).isEqualTo("Hid");
    assertThat(ncfile.getLocation()).isEqualTo("location");
    assertThat(ncfile.getTitle()).isEqualTo("title");

    Formatter f = new Formatter();
    ncfile.getDetailInfo(f);
    assertThat(f.toString()).startsWith("CdmDataset location= location");

    Group group = ncfile.getRootGroup();
    assertThat(group.getCdmFile()).isEqualTo(ncfile);
    assertThat(group.getShortName()).isEqualTo("");
    assertThat(group.isRoot()).isTrue();
    assertThat(group.attributes()).isNotEmpty();
    assertThat(group.attributes()).hasSize(1);
    assertThat(group.findAttribute("attName")).isEqualTo(att);
    assertThat(group.findAttributeString("attName", null)).isEqualTo("value");

    assertThat(group.getDimensions()).isNotEmpty();
    assertThat(group.getDimensions()).hasSize(1);
    assertThat(group.findDimension("dimName").isPresent()).isTrue();
    assertThat(group.findDimension("dimName").get()).isEqualTo(dim);

    assertThat(group.getGroups()).isNotEmpty();
    assertThat(group.getGroups()).hasSize(1);
    Group child = group.findGroupLocal("child");
    assertThat(child.getParentGroup()).isEqualTo(group);

    assertThat(group.getVariables()).isNotEmpty();
    assertThat(group.getVariables()).hasSize(1);
    Variable v = group.findVariableLocal("varName");
    assertThat(v.getParentGroup()).isEqualTo(group);
    assertThat(v.getCdmFile()).isEqualTo(ncfile);
  }

  @Test
  public void testCoordinatesHelper() {
    CdmDatasetCS.Builder<?> ncdb = CdmDatasetCS.builder();
    CoordsHelperBuilder coords = new CoordsHelperBuilder("testCoordinatesHelper");
    ncdb.coords = coords;

    VariableDS.Builder<?> xBuilder = VariableDS.builder().setName("xname").setArrayType(ArrayType.FLOAT)
        .setUnits("xunits").setDesc("xdesc").setEnhanceMode(CdmDataset.getEnhanceAll());
    coords.addCoordinateAxis(CoordinateAxis.fromVariableDS(xBuilder).setAxisType(AxisType.GeoX));

    VariableDS.Builder<?> yBuilder = VariableDS.builder().setName("yname").setArrayType(ArrayType.FLOAT)
        .setUnits("yunits").setDesc("ydesc").setEnhanceMode(CdmDataset.getEnhanceAll());
    coords.addCoordinateAxis(CoordinateAxis.fromVariableDS(yBuilder).setAxisType(AxisType.GeoY));

    CoordinateSystem.Builder<?> csb = CoordinateSystem.builder("xname yname").setCoordAxesNames("xname yname");
    coords.addCoordinateSystem(csb);

    CdmDatasetCS ncd = ncdb.build();
    CoordinateSystem coordSys = ncd.findCoordinateSystem("xname yname");
    assertThat(coordSys).isNotNull();

    CoordinateAxis xaxis = coordSys.findAxis(AxisType.GeoX);
    assertThat(xaxis).isNotNull();
    assertThat(xaxis.getShortName()).isEqualTo("xname");
    assertThat(xaxis.getArrayType()).isEqualTo(ArrayType.FLOAT);
    assertThat(xaxis.getUnitsString()).isEqualTo("xunits");
    assertThat(xaxis.getDescription()).isEqualTo("xdesc");
    assertThat(xaxis.getEnhanceMode()).isEqualTo(CdmDataset.getEnhanceAll());
    assertThat(xaxis.findAttributeString(CDM.UNITS, "")).isEqualTo("xunits");
    assertThat(xaxis.findAttributeString(CDM.LONG_NAME, "")).isEqualTo("xdesc");

    CoordinateAxis xaxis2 = ncd.findCoordinateAxis("xname");
    assertThat(xaxis2).isNotNull();
    assertThat(xaxis2).isEqualTo(xaxis);

  }

}
