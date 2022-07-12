/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.collection;

import dev.ucdm.core.api.Attribute;
import dev.ucdm.core.api.AttributeContainerMutable;
import dev.ucdm.core.constants.DataFormatType;
import dev.ucdm.core.constants.CDM;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

import dev.ucdm.grib.common.GribConfig;
import dev.ucdm.grib.common.GribConstants;
import dev.ucdm.grib.common.GribIosp;
import dev.ucdm.grib.common.util.GribNumbers;
import dev.ucdm.grib.common.util.GribUtils;
import dev.ucdm.grib.coord.CoordinateTimeAbstract;
import dev.ucdm.grib.grib1.iosp.Grib1Iosp;
import dev.ucdm.grib.grib1.iosp.Grib1Parameter;
import dev.ucdm.grib.grib1.table.Grib1Customizer;

/**
 * Grib1-specific subclass of GribCollection.
 *
 * @author John
 * @since 9/5/11
 */
public class Grib1Collection extends GribCollection {

  public Grib1Collection(String name, File directory, GribConfig config) {
    super(name, directory, config, true);
  }

  @Override
  public GribIosp makeIosp() throws IOException {
    GribIosp result = new Grib1Iosp(this);
    result.createCustomizer();
    return result;
  }

  @Override
  public void addGlobalAttributes(AttributeContainerMutable result) {
    String val = cust.getGeneratingProcessName(getGenProcessId());
    if (val != null)
      result.addAttribute(new Attribute(GribUtils.GEN_PROCESS, val));
    result.addAttribute(new Attribute(CDM.FILE_FORMAT, DataFormatType.GRIB1.getDescription()));
  }

  @Override
  public String makeVariableId(VariableIndex v) {
    return makeVariableId(getCenter(), getSubcenter(), v.getTableVersion(), v.getParameter(), v.getLevelType(),
        v.isLayer(), v.getIntvType(), v.getTimeIntvName());
  }

  static String makeVariableId(int center, int subcenter, int tableVersion, int paramNo, int levelType, boolean isLayer,
      int intvType, String intvName) {
    try (Formatter f = new Formatter()) {

      f.format("VAR_%d-%d-%d-%d", center, subcenter, tableVersion, paramNo); // "VAR_7-15--1-20_L1";

      if (levelType != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
        f.format("_L%d", levelType); // code table 4.5
        if (isLayer) {
          f.format("_layer");
        }
      }

      if (intvType >= 0) {
        if (intvName != null) {
          if (intvName.equals(CoordinateTimeAbstract.MIXED_INTERVALS)) {
            f.format("_Imixed");
          } else {
            f.format("_I%s", intvName);
          }
        }
        f.format("_S%s", intvType);
      }

      return f.toString();
    }
  }

  @Override
  public void addVariableAttributes(AttributeContainerMutable v, VariableIndex vindex) {
    addVariableAttributes(v, vindex, this);
  }

  static void addVariableAttributes(AttributeContainerMutable v, VariableIndex vindex, GribCollection gc) {
    Grib1Customizer cust1 = (Grib1Customizer) gc.cust;

    // Grib attributes
    v.addAttribute(new Attribute(GribConstants.VARIABLE_ID_ATTNAME, gc.makeVariableId(vindex)));
    v.addAttribute(new Attribute("Grib1_Center", gc.getCenter()));
    v.addAttribute(new Attribute("Grib1_Subcenter", gc.getSubcenter()));
    v.addAttribute(new Attribute("Grib1_TableVersion", vindex.getTableVersion()));
    v.addAttribute(new Attribute("Grib1_Parameter", vindex.getParameter()));
    Grib1Parameter param =
        cust1.getParameter(gc.getCenter(), gc.getSubcenter(), vindex.getTableVersion(), vindex.getParameter());
    if (param != null && param.getName() != null)
      v.addAttribute(new Attribute("Grib1_Parameter_Name", param.getName()));

    if (vindex.getLevelType() != GribNumbers.MISSING)
      v.addAttribute(new Attribute("Grib1_Level_Type", vindex.getLevelType()));
    String ldesc = cust1.getLevelDescription(vindex.getLevelType());
    if (ldesc != null)
      v.addAttribute(new Attribute("Grib1_Level_Desc", ldesc));


    String timeTypeName = cust1.getTimeTypeName(vindex.getIntvType());
    if (timeTypeName != null && !timeTypeName.isEmpty()) {
      v.addAttribute(new Attribute("Grib1_Interval_Type", vindex.getIntvType()));
      v.addAttribute(new Attribute("Grib1_Interval_Name", timeTypeName));
    }

    if (vindex.getEnsDerivedType() >= 0)
      v.addAttribute(new Attribute("Grib1_Ensemble_Derived_Type", vindex.getEnsDerivedType()));
    else if (vindex.getProbabilityName() != null && !vindex.getProbabilityName().isEmpty())
      v.addAttribute(new Attribute("Grib1_Probability_Type", vindex.getProbabilityName()));
  }

}
