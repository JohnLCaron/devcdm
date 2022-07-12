/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.api;

import com.google.common.collect.ImmutableList;
import dev.ucdm.array.ArrayType;
import dev.ucdm.core.api.*;
import dev.ucdm.core.constants.AxisType;
import dev.ucdm.dataset.transform.horiz.ProjectionFactory;
import dev.ucdm.dataset.geoloc.Projection;
import dev.ucdm.dataset.geoloc.LatLonProjection;
import dev.ucdm.core.util.StringUtil2;

import dev.ucdm.dataset.transform.vertical.VerticalTransform;
import org.jetbrains.annotations.Nullable;
import dev.ucdm.array.Immutable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Specifies the coordinates of a Variable's values,
 * this is a legacy class, use GridCoordinateSystem for new code.
 *
 * <pre>
 * Mathematically it is a vector function F from index space to Sn:
 *  F(i,j,k,...) -&gt; (S1, S2, ...Sn)
 *  where i,j,k are integers, and S is the set of reals (R) or Strings.
 * </pre>
 * 
 * The components of F are just its coordinate axes:
 * 
 * <pre>
 *  F = (A1, A2, ...An)
 *    A1(i,j,k,...) -&gt; S1
 *    A2(i,j,k,...) -&gt; S2
 *    An(i,j,k,...) -&gt; Sn
 * </pre>
 *
 * Concretely, a CoordinateSystem is a set of coordinate axes, and an optional set of coordinate transforms.
 * The domain rank of F is the number of dimensions it is a function of.
 * The range rank is the number of coordinate axes.
 *
 * <p>
 * An important class of CoordinateSystems are <i>georeferencing</i> Coordinate Systems, that locate a
 * Variable's values in space and time. A CoordinateSystem that has a Lat and Lon axis, or a GeoX and GeoY
 * axis and a Projection CoordinateTransform will have <i>isGeoReferencing()</i> true.
 */
@Immutable
public class CoordinateSystem {

  /**
   * Create standard name from list of axes. Sort the axes first
   * 
   * @param axes list of CoordinateAxis
   * @return CoordinateSystem name, created from axes names
   */
  public static String makeName(List<CoordinateAxis> axes) {
    List<CoordinateAxis> axesSorted = new ArrayList<>(axes);
    axesSorted.sort(new CoordinateAxis.AxisComparator());
    ArrayList<String> names = new ArrayList<>();
    axesSorted.forEach(axis -> names.add(axis.getShortName()));
    return String.join(" ", names);
  }

  /** Get the List of CoordinateAxes */
  public List<CoordinateAxis> getCoordinateAxes() {
    return coordAxes;
  }

  public List<CoordinateTransform> getCoordinateTransforms() {
    return coordTransforms;
  }


  /** Get the name of the Coordinate System */
  public String getName() {
    return name;
  }

  /** Get the canonical name of the Coordinate System */
  public String getAxesName() {
    return makeName(this.coordAxes);
  }

  /** Get the Collection of Dimensions used by any of the CoordinateAxes. */
  public Collection<Dimension> getDomain() {
    return ImmutableList.copyOf(domain);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Convenience routines for finding georeferencing axes

  /**
   * Find the CoordinateAxis that has the given AxisType.
   * If more than one, return the one with lesser rank.
   * 
   * @param type look for this axisType
   * @return CoordinateAxis of the given AxisType, else null.
   */
  @Nullable
  public CoordinateAxis findAxis(AxisType type) {
    CoordinateAxis result = null;
    for (CoordinateAxis axis : coordAxes) {
      AxisType axisType = axis.getAxisType();
      if ((axisType != null) && (axisType == type)) {
        result = lesserRank(result, axis);
      }
    }
    return result;
  }

  // prefer smaller ranks, in case there's more than one
  private CoordinateAxis lesserRank(CoordinateAxis a1, CoordinateAxis a2) {
    if (a1 == null) {
      return a2;
    }
    return (a1.getRank() <= a2.getRank()) ? a1 : a2;
  }

  /** Find CoordinateAxis of one of the given types, in the order given. */
  @Nullable
  public CoordinateAxis findAxis(AxisType... axisType) {
    for (AxisType type : axisType) {
      CoordinateAxis result = findAxis(type);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /** Get the Projection for this coordinate system. */
  @Nullable
  public Projection getProjection() {
    if (projection == null) {
      this.coordTransforms.stream()
              .filter( it -> it.isProjection())
              .findFirst()
              .ifPresent( ctv ->
                      this.projection = ProjectionFactory.makeProjection(ctv, new Formatter()));
    }
    return projection;
  }

  @Nullable
  public CoordinateTransform getVerticalTransform() {
      return this.coordTransforms.stream().filter( it -> !it.isProjection()).findFirst().orElse(null);
  }

  ////////////////////////////////////////////////////////////////////////////
  // classification

  /** True if it has X and Y CoordinateAxis, and a Projection */
  public boolean isGeoXY() {
    if ((xAxis == null) || (yAxis == null)) {
      return false;
    }
    return null != getProjection() && !(projection instanceof LatLonProjection);
  }

  /** True if it has Lat and Lon CoordinateAxis */
  public boolean isLatLon() {
    return (latAxis != null) && (lonAxis != null);
  }

  /** True if isGeoXY or isLatLon */
  public boolean isGeoReferencing() {
    return isGeoXY() || isLatLon();
  }

  /**
   * Check if this Coordinate System is complete for v, ie if all v's dimensions are used by the Coordinate System.
   * Exclude dimensions with length &lt; 2.
   */
  public boolean isComplete(Variable v) {
    return isComplete(v.getDimensionSet(), domain);
  }

  /** True if all variableDomain dimensions are contained in csysDomain, or have length &lt; 2. */
  public static boolean isComplete(Collection<Dimension> variableDomain, Collection<Dimension> csysDomain) {
    for (Dimension d : variableDomain) {
      if (!(csysDomain.contains(d)) && (d.getLength() > 1)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Check if this Coordinate System can be used for the given variable, by checking if each CoordinateAxis
   * can be used for the Variable.
   * A CoordinateAxis can only be used if the CoordinateAxis' set of Dimensions is a
   * subset of the Variable's set of Dimensions.
   */
  public boolean isCoordinateSystemFor(Variable v) {
    HashSet<Dimension> varDims = new HashSet<>(v.getDimensions());
    for (CoordinateAxis axis : getCoordinateAxes()) {
      Group groupv = v.getParentGroup();
      Group groupa = axis.getParentGroup();
      Group commonGroup = groupv.commonParent(groupa);

      // a CHAR variable must really be a STRING, so leave out the last (string length) dimension
      int checkDims = axis.getRank();
      if (axis.getArrayType() == ArrayType.CHAR) {
        checkDims--;
      }
      for (int i = 0; i < checkDims; i++) {
        Dimension axisDim = axis.getDimension(i);
        if (!axisDim.isShared()) { // anon dimensions dont count. TODO does this work?
          continue;
        }
        if (!varDims.contains(axisDim)) {
          return false;
        }
        // The dimension must be in the common parent group
        if (groupa != groupv && commonGroup.findDimension(axisDim) == null) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Do we have the named axis?
   * 
   * @param axisName name of axis: check short then full name
   * @return true if we have an axis of that name
   */
  public boolean containsAxis(String axisName) {
    for (CoordinateAxis ca : coordAxes) {
      if (ca.getShortName().equals(axisName))
        return true;
    }
    for (CoordinateAxis ca : coordAxes) {
      if (ca.getFullName().equals(axisName))
        return true;
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CoordinateSystem that = (CoordinateSystem) o;
    return coordAxes.equals(that.coordAxes) && coordTransforms.equals(that.coordTransforms) && name.equals(that.name) && domain.equals(that.domain);
  }

  @Override
  public int hashCode() {
    return Objects.hash(coordAxes, coordTransforms, name, domain);
  }

  @Override
  public String toString() {
    return name;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  private final ImmutableList<CoordinateAxis> coordAxes;
  private final List<CoordinateTransform> coordTransforms;
  private final boolean isImplicit;
  private Projection projection; // lazy
  private VerticalTransform verticalTransform; // lazy

  // these are calculated
  private final String name;
  private final Set<Dimension> domain = new HashSet<>(); // set of dimension
  private final CoordinateAxis xAxis, yAxis, zAxis, tAxis, latAxis, lonAxis, hAxis, pAxis, ensAxis;
  private final CoordinateAxis aziAxis, elevAxis, radialAxis;

  protected CoordinateSystem(Builder<?> builder, List<CoordinateAxis> axesAll, List<CoordinateTransform> allTransforms) {
    this.isImplicit = builder.isImplicit;

    // find referenced coordinate axes
    List<CoordinateAxis> axesList = new ArrayList<>();
    for (String axisName : StringUtil2.split(builder.coordAxesNames)) {
      Optional<CoordinateAxis> found = axesAll.stream().filter(axis -> axisName.equals(axis.getFullName())).findFirst();
      if (found.isEmpty()) {
        found = axesAll.stream().filter(axis -> axisName.equals(axis.getShortName())).findFirst();
      }
      if (found.isEmpty()) {
          throw new RuntimeException("Cant find axis " + axisName);
      } else {
        axesList.add(found.get());
      }
    }
    axesList.sort(new CoordinateAxis.AxisComparator());
    this.coordAxes = ImmutableList.copyOf(axesList);

    // calculated
    this.name = builder.name;

    CoordinateAxis xAxis = null, yAxis = null, zAxis = null, tAxis = null, latAxis = null, lonAxis = null;
    CoordinateAxis hAxis = null, pAxis = null, ensAxis = null;
    CoordinateAxis aziAxis = null, elevAxis = null, radialAxis = null;

    for (CoordinateAxis axis : this.coordAxes) {
      // look for AxisType
      AxisType axisType = axis.getAxisType();
      if (axisType != null) {
        if (axisType == AxisType.GeoX)
          xAxis = lesserRank(xAxis, axis);
        if (axisType == AxisType.GeoY)
          yAxis = lesserRank(yAxis, axis);
        if (axisType == AxisType.GeoZ)
          zAxis = lesserRank(zAxis, axis);
        if (axisType == AxisType.Time)
          tAxis = lesserRank(tAxis, axis);
        if (axisType == AxisType.Lat)
          latAxis = lesserRank(latAxis, axis);
        if (axisType == AxisType.Lon)
          lonAxis = lesserRank(lonAxis, axis);
        if (axisType == AxisType.Height)
          hAxis = lesserRank(hAxis, axis);
        if (axisType == AxisType.Pressure)
          pAxis = lesserRank(pAxis, axis);
        if (axisType == AxisType.Ensemble)
          ensAxis = lesserRank(ensAxis, axis);

        if (axisType == AxisType.RadialAzimuth)
          aziAxis = lesserRank(aziAxis, axis);
        if (axisType == AxisType.RadialDistance)
          radialAxis = lesserRank(radialAxis, axis);
        if (axisType == AxisType.RadialElevation)
          elevAxis = lesserRank(elevAxis, axis);
      }
      // collect dimensions
      domain.addAll(Dimensions.makeDimensionsAll(axis));
    }

    this.xAxis = xAxis;
    this.yAxis = yAxis;
    this.zAxis = zAxis;
    this.tAxis = tAxis;
    this.latAxis = latAxis;
    this.lonAxis = lonAxis;
    this.hAxis = hAxis;
    this.pAxis = pAxis;
    this.ensAxis = ensAxis;
    this.aziAxis = aziAxis;
    this.elevAxis = elevAxis;
    this.radialAxis = radialAxis;

    // Find the named coordinate transforms in allTransforms.
    this.coordTransforms = allTransforms.stream().filter( ct -> builder.transformNames.contains(ct.name())).toList();
    /*

    List<CoordinateTransform> cts = new ArrayList<>();
    allTransforms.stream().filter( ct -> builder.transformNames.contains(ct.name())).forEach(it -> cts.add(it));
    // add the projection
    if (builder.projName != null) {
      allTransforms.stream().filter(ct -> builder.projName.equals(ct.name())).findFirst().ifPresent( it -> cts.add(it));
    }
    this.coordTransforms = ImmutableList.copyOf(cts); */
  }

  /** Convert to a mutable Builder. */
  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder(this.name));
  }

  // Add local fields to the passed - in builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    b.setImplicit(this.isImplicit).setCoordAxesNames(this.name);
    this.coordTransforms.stream().forEach( it -> b.addTransformName(it.name()));
    return b;
  }

  /** Get a Builder of CoordinateSystem */
  public static Builder<?> builder(String name) {
    return new Builder2().setName(name);
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> {
    public String name;
    public String coordAxesNames = "";
    private String projName;
    private List<String> transformNames = new ArrayList<>();
    private boolean isImplicit;
    private boolean built;

    protected abstract T self();

    public T setName(String name) {
      this.name = name;
      return self();
    }

    /** @param names list of axes names, space delimited. Doesnt have to be sorted. */
    public T setCoordAxesNames(String names) {
      this.coordAxesNames = names;
      return self();
    }

    // LOOK get rid of when old coordsys goes away
    public T setProjectionName(String projName) {
      this.projName = projName;
      return self();
    }

    // return true if didnt already have this name
    public boolean addTransformName(String ct) {
      if (transformNames.contains(ct)) {
        return false;
      } else {
        transformNames.add(ct);
        return true;
      }
    }

    public T setImplicit(boolean isImplicit) {
      this.isImplicit = isImplicit;
      return self();
    }

    public boolean containsAxes(List<String> axes) {
      return axes.stream().allMatch(it -> coordAxesNames.contains(it));
    }

    /**
     * Build a CoordinateSystem
     * 
     * @param axes Must contain all axes that are named in coordAxesNames
     * @param transforms Must contain any transforms that are named by setCoordinateTransformName
     */
    public CoordinateSystem build(List<CoordinateAxis> axes, List<CoordinateTransform> transforms) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new CoordinateSystem(this, axes, transforms);
    }
  }

}
