/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.dataset.conv;

import dev.cdm.core.api.CdmFile;
import dev.cdm.core.calendar.CalendarDateUnit;
import dev.cdm.dataset.api.CdmDatasetCS;
import dev.cdm.dataset.spi.CoordSystemBuilderProvider;
import dev.cdm.array.Array;
import dev.cdm.array.ArrayType;
import dev.cdm.core.api.Attribute;
import dev.cdm.core.api.Dimension;
import dev.cdm.core.api.Variable;
import dev.cdm.core.calendar.CalendarDate;
import dev.cdm.core.constants.AxisType;
import dev.cdm.core.constants.CF;
import dev.cdm.core.constants._Coordinate;
import dev.cdm.dataset.api.SimpleUnit;
import dev.cdm.dataset.api.VariableDS;
import dev.cdm.dataset.internal.CoordSystemBuilder;
import dev.cdm.dataset.transform.horiz.ProjectionCTV;
import dev.cdm.core.util.CancelTask;
import dev.cdm.dataset.geoloc.Projection;
import dev.cdm.dataset.geoloc.projection.LambertConformal;
import dev.cdm.dataset.geoloc.projection.Stereographic;
import dev.cdm.dataset.geoloc.projection.TransverseMercator;

import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Default Coordinate Conventions. Used when no other is specified or recognized. */
public class DefaultConventions extends CoordSystemBuilder {
  private static final Logger logger = LoggerFactory.getLogger(DefaultConventions.class);

  protected ProjectionCTV projCT;

  private DefaultConventions(CdmDatasetCS.Builder<?> datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = "Default";
  }

  @Override
  public void augmentDataset(CancelTask cancelTask) {
    projCT = makeProjectionCT();
    if (projCT != null) {
      VariableDS.Builder<?> vb = makeCoordinateTransformVariable(projCT);
      rootGroup.addVariable(vb);

      String xname = findCoordinateName(AxisType.GeoX);
      String yname = findCoordinateName(AxisType.GeoY);
      if (xname != null && yname != null) {
        vb.addAttribute(new Attribute(_Coordinate.Axes, xname + " " + yname));
      }
    }
  }

  @Override
  protected void identifyCoordinateAxes() {

    // Look for coord_axis or coord_alias attribute
    for (VarProcess vp : varList) {
      if (vp.isCoordinateVariable) {
        continue;
      }

      String dimName = findAlias(vp.vb);
      if (dimName.isEmpty()) {
        continue;
      }
      Optional<Dimension> dimOpt = this.rootGroup.findDimension(dimName);
      dimOpt.ifPresent(dime -> {
        vp.isCoordinateAxis = true;
        parseInfo.format(" Coordinate Axis added (alias) = %s for dimension %s%n", vp, dimName);
      });
    }

    // coordinates is an alias for _CoordinateAxes
    for (VarProcess vp : varList) {
      if (vp.coordinateAxes == null) { // dont override if already set
        String coordsString = vp.vb.getAttributeContainer().findAttributeString(CF.COORDINATES, null);
        if (coordsString != null) {
          vp.coordinates = coordsString;
        }
      }
    }

    super.identifyCoordinateAxes();

    /////////////////////////
    // now we start forcing
    HashMap<AxisType, VarProcess> map = new HashMap<>();

    // find existing axes, so we dont duplicate
    for (VarProcess vp : varList) {
      if (vp.isCoordinateAxis) {
        AxisType atype = getAxisType(vp.vb);
        if (atype != null) {
          map.put(atype, vp);
        }
      }
    }

    // look for time axes based on units
    if (map.get(AxisType.Time) == null) {
      for (VarProcess vp : varList) {
        String unit = vp.vb.getUnits();
        if (unit != null && CalendarDateUnit.isDateUnit(unit)) {
          vp.isCoordinateAxis = true;
          map.put(AxisType.Time, vp);
          parseInfo.format(" Time Coordinate Axis added (unit) = %s from unit %s%n", vp.vb.getFullName(), unit);
          // break; // allow multiple time coords
        }
      }
    }

    // look for missing axes by using name hueristics
    for (VarProcess vp : varList) {
      if (vp.isCoordinateVariable) {
        continue;
      }
      AxisType atype = getAxisType(vp.vb);
      if (atype != null) {
        if (map.get(atype) == null) {
          vp.isCoordinateAxis = true;
          parseInfo.format(" Coordinate Axis added (Default forced) = %s for axis %s%n", vp.vb.getFullName(), atype);
          map.put(atype, vp);
        }
      }
    }
  }

  /**
   * look for aliases.
   *
   * @param axisType look for this axis type
   * @return name of axis of that type
   */
  @Nullable
  private String findCoordinateName(AxisType axisType) {
    for (Variable.Builder<?> vb : rootGroup.vbuilders) {
      if (vb instanceof VariableDS.Builder) {
        VariableDS.Builder<?> vds = (VariableDS.Builder<?>) vb;
        if (axisType == getAxisType(vds)) {
          return vds.getFullName();
        }
      }
    }
    return null;
  }

  @Override
  @Nullable
  protected AxisType getAxisType(VariableDS.Builder<?> vb) {
    AxisType result = getAxisTypeCoards(vb);
    if (result != null) {
      return result;
    }

    String vname = vb.shortName;
    if (vname == null) {
      return null;
    }
    String unit = vb.getUnits();
    if (unit == null) {
      unit = "";
    }
    String desc = vb.getDescription();
    if (desc == null) {
      desc = "";
    }

    if (vname.equalsIgnoreCase("x") || findAlias(vb).equalsIgnoreCase("x")) {
      return AxisType.GeoX;
    }

    if (vname.equalsIgnoreCase("lon") || vname.equalsIgnoreCase("longitude") || findAlias(vb).equalsIgnoreCase("lon")) {
      return AxisType.Lon;
    }

    if (vname.equalsIgnoreCase("y") || findAlias(vb).equalsIgnoreCase("y")) {
      return AxisType.GeoY;
    }

    if (vname.equalsIgnoreCase("lat") || vname.equalsIgnoreCase("latitude") || findAlias(vb).equalsIgnoreCase("lat")) {
      return AxisType.Lat;
    }

    if (vname.equalsIgnoreCase("lev") || findAlias(vb).equalsIgnoreCase("lev")
        || (vname.equalsIgnoreCase("level") || findAlias(vb).equalsIgnoreCase("level"))) {
      return AxisType.GeoZ;
    }

    if (vname.equalsIgnoreCase("z") || findAlias(vb).equalsIgnoreCase("z") || vname.equalsIgnoreCase("altitude")
        || desc.contains("altitude") || vname.equalsIgnoreCase("depth") || vname.equalsIgnoreCase("elev")
        || vname.equalsIgnoreCase("elevation")) {
      if (SimpleUnit.isCompatible("m", unit)) { // units of meters
        return AxisType.Height;
      }
    }

    if (vname.equalsIgnoreCase("time") || findAlias(vb).equalsIgnoreCase("time")) {
      if (CalendarDateUnit.isDateUnit(unit)) {
        return AxisType.Time;
      }
    }

    if (vname.equalsIgnoreCase("time") && vb.dataType == ArrayType.STRING) {
      if (vb.orgVar != null) {
        try {
          Array<?> values = vb.orgVar.readArray();
          if (values.getArrayType() == ArrayType.STRING) {
            Array<String> svalues = (Array<String>) values;
            String firstStringValue = svalues.getScalar();
            if (CalendarDate.fromUdunitIsoDate(null, firstStringValue).isPresent()) {
              return AxisType.Time;
            }
          }
        } catch (IOException | IllegalArgumentException e) {
          logger.warn("time string error", e);
        }
      } else {
        return AxisType.Time; // kludge: see aggSynGrid.xml example test
      }
    }

    return null;
  }

  // look for an coord_axis or coord_alias attribute
  private String findAlias(VariableDS.Builder<?> vb) {
    String alias = vb.getAttributeContainer().findAttributeString("coord_axis", null);
    if (alias == null) {
      alias = vb.getAttributeContainer().findAttributeString("coord_alias", "");
    }
    if (alias == null) {
      alias = "";
    }
    return alias;
  }

  // replicated from COARDS, but we need to diverge from COARDS
  // we assume that coordinate axes get identified by being coordinate variables
  @Nullable
  private AxisType getAxisTypeCoards(VariableDS.Builder<?> vb) {
    String unit = vb.getUnits();
    if (unit == null) {
      return null;
    }

    if (unit.equalsIgnoreCase("degrees_east") || unit.equalsIgnoreCase("degrees_E") || unit.equalsIgnoreCase("degreesE")
        || unit.equalsIgnoreCase("degree_east") || unit.equalsIgnoreCase("degree_E")
        || unit.equalsIgnoreCase("degreeE")) {
      return AxisType.Lon;
    }

    if (unit.equalsIgnoreCase("degrees_north") || unit.equalsIgnoreCase("degrees_N")
        || unit.equalsIgnoreCase("degreesN") || unit.equalsIgnoreCase("degree_north")
        || unit.equalsIgnoreCase("degree_N") || unit.equalsIgnoreCase("degreeN")) {
      return AxisType.Lat;
    }

    if (CalendarDateUnit.isDateUnit(unit)) { // || SimpleUnit.isTimeUnit(unit)) removed dec 18, 2008
      return AxisType.Time;
    }

    // look for other z coordinate
    // if (SimpleUnit.isCompatible("m", unit))
    // return AxisType.Height;
    if (SimpleUnit.isCompatible("mbar", unit)) {
      return AxisType.Pressure;
    }
    if (unit.equalsIgnoreCase("level") || unit.equalsIgnoreCase("layer") || unit.equalsIgnoreCase("sigma_level")) {
      return AxisType.GeoZ;
    }

    String positive = vb.getAttributeContainer().findAttributeString("positive", null);
    if (positive != null) {
      if (SimpleUnit.isCompatible("m", unit)) {
        return AxisType.Height;
      } else {
        return AxisType.GeoZ;
      }
    }
    return null;
  }

  private ProjectionCTV makeProjectionCT() {
    // look for projection in global attribute
    String projection = rootGroup.getAttributeContainer().findAttributeString("projection", null);
    if (null == projection) {
      parseInfo.format("Default Conventions error: NO projection name found %n");
      return null;
    }
    String params = rootGroup.getAttributeContainer().findAttributeString("projection_params", null);
    if (null == params) {
      params = rootGroup.getAttributeContainer().findAttributeString("proj_params", null);
    }
    if (null == params) {
      parseInfo.format("Default Conventions error: NO projection parameters found %n");
      return null;
    }

    // parse the parameters
    int count = 0;
    double[] p = new double[4];
    try {
      // new way : just the parameters
      StringTokenizer stoke = new StringTokenizer(params, " ,");
      while (stoke.hasMoreTokens() && (count < 4)) {
        p[count++] = Double.parseDouble(stoke.nextToken());
      }
    } catch (NumberFormatException e) {
      // old way : every other one
      StringTokenizer stoke = new StringTokenizer(params, " ,");
      while (stoke.hasMoreTokens() && (count < 4)) {
        stoke.nextToken(); // skip
        p[count++] = Double.parseDouble(stoke.nextToken());
      }
    }

    parseInfo.format("Default Conventions projection %s params = %f %f %f %f%n", projection, p[0], p[1], p[2], p[3]);

    Projection proj;
    if (projection.equalsIgnoreCase("LambertConformal")) {
      proj = new LambertConformal(p[0], p[1], p[2], p[3]);
    } else if (projection.equalsIgnoreCase("TransverseMercator")) {
      proj = new TransverseMercator(p[0], p[1], p[2]);
    } else if (projection.equalsIgnoreCase("Stereographic") || projection.equalsIgnoreCase("Oblique_Stereographic")) {
      proj = new Stereographic(p[0], p[1], p[2]);
    } else {
      parseInfo.format("Default Conventions error: Unknown projection %s%n", projection);
      return null;
    }

    return new ProjectionCTV(proj.getClassName(), proj);
  }

  public static class Factory implements CoordSystemBuilderProvider {
    @Override
    @Nullable
    public String getConventionName() {
      return null;
    }

    public boolean isMine(CdmFile ncfile) {
      // this is to test DefaultConventions, not needed when we remove old convention builders.
      // return ncfile.getLocation().endsWith("amsr-avhrr-v2.20040729.nc");
      return false;
    }

    @Override
    public CoordSystemBuilder open(CdmDatasetCS.Builder<?> datasetBuilder) {
      return new DefaultConventions(datasetBuilder);
    }
  }

}


