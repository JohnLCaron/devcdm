/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.dataset.internal;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import dev.ucdm.dataset.api.*;
import dev.ucdm.dataset.coordsysbuild.CoordsHelperBuilder;

import dev.ucdm.array.Immutable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** An immutable helper class for NetcdfDataset to build and manage coordinates. */
@Immutable
public class CoordinatesHelper implements Coordinates {

  //////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public String getConventionName() {
    return conventionName;
  }

  @Override
  public List<CoordinateAxis> getCoordinateAxes() {
    return coordAxes;
  }

  @Override
  public List<CoordinateSystem> getCoordinateSystems() {
    return coordSystems;
  }

  @Override
  public List<CoordinateTransform> getCoordinateTransforms() {
    return coordTransforms;
  }

  @Nullable
  public CoordinateSystem findCoordinateSystem(String name) {
    Preconditions.checkNotNull(name);
    return coordSystems.stream().filter(cs -> cs.getName().equals(name)).findFirst().orElse(null);
  }

  @Override
  public List<CoordinateSystem> makeCoordinateSystemsFor(VariableDS v) {
    String fullName = v.getFullName();
    List<String> names = coordSysForVar.get(fullName);
    if (names != null) {
      return names.stream().map(this::findCoordinateSystem).filter(Objects::nonNull).toList();
    }

    ArrayList<CoordinateSystem> result = new ArrayList<>();
    for (CoordinateSystem csys : coordSystems) {
      if (csys.isCoordinateSystemFor(v) && csys.isComplete(v)) {
        result.add(csys);
      }
    }
    result.sort((cs1, cs2) -> cs2.getCoordinateAxes().size() - cs1.getCoordinateAxes().size());
    return ImmutableList.copyOf(result);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  private final String conventionName;
  private final ImmutableList<CoordinateAxis> coordAxes;
  private final ImmutableList<CoordinateSystem> coordSystems;
  private final ImmutableList<CoordinateTransform> coordTransforms;
  private final Map<String, List<String>> coordSysForVar;

  public CoordinatesHelper(CoordsHelperBuilder builder, List<CoordinateAxis> axes) {
    this.conventionName = builder.getConventionName();
    this.coordAxes = ImmutableList.copyOf(axes);
    this.coordTransforms = ImmutableList.copyOf(builder.getCoordTransforms());

    // TODO remove coordSys not used by a variable....
    this.coordSystems = builder.getCoordSys().stream().map(csb -> csb.build(this.coordAxes, this.coordTransforms))
        .filter(Objects::nonNull).collect(ImmutableList.toImmutableList());

    this.coordSysForVar = builder.getCoordinateSystemFor();
  }

}
