package dev.ucdm.grid.internal;

import com.google.common.collect.ImmutableList;
import dev.ucdm.core.api.AttributeContainerMutable;
import dev.ucdm.core.api.Group;
import dev.ucdm.core.constants.CDM;
import dev.ucdm.dataset.api.CoordinateTransform;
import org.junit.Test;
import dev.ucdm.array.ArrayType;
import dev.ucdm.core.constants.AxisType;
import dev.ucdm.dataset.api.CoordinateAxis;
import dev.ucdm.dataset.api.CoordinateSystem;
import dev.ucdm.dataset.api.CdmDataset;
import dev.ucdm.dataset.api.VariableDS;
import dev.ucdm.grid.api.*;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link GridNetcdfCSBuilder} */
public class TestGridNetcdfCSBuilder {

  public static Group makeDummyGroup() {
    return Group.builder().setName("").build();
  }

  @Test
  public void testBasics() {
    // CdmDataset
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

    CoordinateSystem.Builder<?> csb =
        CoordinateSystem.builder("csysName").setCoordAxesNames("xname yname").setProjectionName("horiz");
    csb.addTransformName(CDM.FlatEarth);
    CoordinateSystem coordSys = csb.build(axes, allProjs);

    // GridDataset
    GridNetcdfCSBuilder builder = new GridNetcdfCSBuilder();
    builder.setName(coordSys.getName());
    builder.setProjection(coordSys.getProjection());

    for (CoordinateAxis axis : coordSys.getCoordinateAxes()) {
      CoordAxisToGridAxis subject = CoordAxisToGridAxis.create(axis, GridAxisDependenceType.independent, true);
      GridAxis<?> gridAxis = subject.extractGridAxis();
      builder.addAxis(gridAxis);
    }

    GridCoordinateSystem subject = builder.build();
    assertThat(subject.getName()).isEqualTo(coordSys.getAxesName());
    assertThat(subject.getHorizCoordinateSystem().getProjection()).isEqualTo(coordSys.getProjection());

    GridAxis<?> gridAxisX = subject.findAxis("xname").orElseThrow();
    assertThat(gridAxisX.getName()).isEqualTo("xname");
    GridAxisPoint xaxis = subject.getXHorizAxis();
    assertThat(xaxis.getName()).isEqualTo("xname");

    GridAxis<?> gridAxisY = subject.findAxis("yname").orElseThrow();
    assertThat(gridAxisY.getName()).isEqualTo("yname");
    GridAxisPoint yaxis = subject.getYHorizAxis();
    assertThat(yaxis.getName()).isEqualTo("yname");

    assertThat((Object) subject.findCoordAxisByType(AxisType.Ensemble)).isNull();
    assertThat((Object) subject.getEnsembleAxis()).isNull();
    assertThat((Object) subject.getVerticalAxis()).isNull();
    assertThat(subject.getTimeCoordinateSystem()).isNull();

    assertThat(subject.getNominalShape()).isEqualTo(ImmutableList.of(1, 1));
  }
}
