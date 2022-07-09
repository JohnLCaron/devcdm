/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.collection;

import dev.cdm.array.ArrayType;
import dev.cdm.array.Arrays;
import dev.cdm.core.api.*;
import dev.cdm.core.constants.DataFormatType;
import dev.cdm.core.constants.CDM;
import dev.ucdm.grib.common.GribTables;
import dev.ucdm.grib.common.util.GribNumbers;
import dev.ucdm.grib.coord.CoordinateTimeAbstract;
import dev.ucdm.grib.grib2.iosp.GribConfig;
import dev.ucdm.grib.grib2.table.Grib2Tables;

import java.io.File;
import java.util.Formatter;

/** Grib2 specific subclass of GribCollection. */
public class Grib2Collection extends GribCollection {

  public Grib2Collection(String name, File directory, GribConfig config) {
    super(name, directory, config, false);
  }

  @Override
  public void addGlobalAttributes(AttributeContainerMutable result) {
    String val = cust.getGeneratingProcessTypeName(getGenProcessType());
    if (val != null)
      result.addAttribute(new Attribute("Type_of_generating_process", val));
    val = cust.getGeneratingProcessName(getGenProcessId());
    if (val != null)
      result.addAttribute(
          new Attribute("Analysis_or_forecast_generating_process_identifier_defined_by_originating_centre", val));
    val = cust.getGeneratingProcessName(getBackProcessId());
    if (val != null)
      result.addAttribute(new Attribute("Background_generating_process_identifier_defined_by_originating_centre", val));
    result.addAttribute(new Attribute(CDM.FILE_FORMAT, DataFormatType.GRIB2.getDescription()));
  }

  @Override
  public String makeVariableId(VariableIndex vindex) {
    return makeVariableId(vindex, this);
  }

  private static String makeVariableId(VariableIndex vindex, GribCollection gc) {
    try (Formatter f = new Formatter()) {

      f.format("VAR_%d-%d-%d", vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter());

      if (vindex.getGenProcessType() == 6 || vindex.getGenProcessType() == 7) {
        f.format("_error"); // its an "error" type variable - add to name
      }

      if (vindex.getLevelType() != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
        f.format("_L%d", vindex.getLevelType()); // code table 4.5
        if (vindex.isLayer()) {
          f.format("_layer");
        }
      }

      String intvName = vindex.getIntvName();
      if (intvName != null && !intvName.isEmpty()) {
        if (intvName.equals(CoordinateTimeAbstract.MIXED_INTERVALS)) {
          f.format("_Imixed");
        } else {
          f.format("_I%s", intvName);
        }
      }

      if (vindex.getIntvType() >= 0) {
        f.format("_S%s", vindex.getIntvType());
      }

      if (vindex.getEnsDerivedType() >= 0) {
        f.format("_D%d", vindex.getEnsDerivedType());
      } else if (vindex.getProbabilityName() != null && !vindex.getProbabilityName().isEmpty()) {
        String s = vindex.getProbabilityName().replace(".", "p");
        f.format("_Prob_%s", s);
      }

      return f.toString();
    }
  }

  @Override
  public void addVariableAttributes(AttributeContainerMutable v, VariableIndex vindex) {
    addVariableAttributes(v, vindex, this);
  }

  static void addVariableAttributes(AttributeContainerMutable v, VariableIndex vindex, GribCollection gc) {
    Grib2Tables cust2 = (Grib2Tables) gc.cust;

    v.addAttribute(new Attribute(Grib.VARIABLE_ID_ATTNAME, gc.makeVariableId(vindex)));
    int[] param = {vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter()};
    v.addAttribute(Attribute.fromArray("Grib2_Parameter", Arrays.factory(ArrayType.INT, new int[] {3}, param)));
    String disc = cust2.getCodeTableValue("0.0", vindex.getDiscipline());
    if (disc != null)
      v.addAttribute(new Attribute("Grib2_Parameter_Discipline", disc));
    String cat = cust2.getCategory(vindex.getDiscipline(), vindex.getCategory());
    if (cat != null)
      v.addAttribute(new Attribute("Grib2_Parameter_Category", cat));
    GribTables.Parameter entry = cust2.getParameter(vindex);
    if (entry != null)
      v.addAttribute(new Attribute("Grib2_Parameter_Name", entry.getName()));

    if (vindex.getLevelType() != GribNumbers.MISSING)
      v.addAttribute(new Attribute("Grib2_Level_Type", vindex.getLevelType()));
    String ldesc = cust2.getLevelName(vindex.getLevelType());
    if (ldesc != null)
      v.addAttribute(new Attribute("Grib2_Level_Desc", ldesc));

    if (vindex.getEnsDerivedType() >= 0)
      v.addAttribute(new Attribute("Grib2_Ensemble_Derived_Type", vindex.getEnsDerivedType()));
    else if (vindex.getProbabilityName() != null && !vindex.getProbabilityName().isEmpty()) {
      v.addAttribute(new Attribute("Grib2_Probability_Type", vindex.getProbType()));
      v.addAttribute(new Attribute("Grib2_Probability_Name", vindex.getProbabilityName()));
    }

    if (vindex.getGenProcessType() >= 0) {
      String genProcessTypeName = cust2.getGeneratingProcessTypeName(vindex.getGenProcessType());
      if (genProcessTypeName != null)
        v.addAttribute(new Attribute("Grib2_Generating_Process_Type", genProcessTypeName));
      else
        v.addAttribute(new Attribute("Grib2_Generating_Process_Type", vindex.getGenProcessType()));
    }

    String statType = cust2.getStatisticName(vindex.getIntvType());
    if (statType != null) {
      v.addAttribute(new Attribute("Grib2_Statistical_Process_Type", statType));
    }

  }

}
