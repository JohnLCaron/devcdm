/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.grib1.iosp;

import dev.ucdm.grib.common.GribConfig;
import dev.ucdm.grib.grib1.record.Grib1ParamTime;
import dev.ucdm.grib.grib1.record.Grib1Record;
import dev.ucdm.grib.grib1.record.Grib1SectionProductDefinition;
import dev.ucdm.grib.grib1.table.Grib1Customizer;

/**
 * Used to group records into a CDM variable
 * Herein lies the semantics of a variable object identity.
 * Read it and weep.
 *
 * @author caron
 * @since 12/28/2014
 */
public record Grib1Variable(Grib1Customizer cust, Grib1SectionProductDefinition pds, boolean useTableVersion,
                           boolean intvMerge, boolean useCenter, int gdsHash) {

  /**
   * Used when processing the gbx9 files
   *
   * @param cust customizer
   * @param gr grib record
   * @param gdsHashOverride can override the gdsHash, 0 for no override
   * @param useTableVersion use pdss.getTableVersion(), default is false
   * @param intvMerge put all intervals together, default true
   * @param useCenter use center id when param no &gt; 127, default is false
   */
  public Grib1Variable(Grib1Customizer cust, Grib1Record gr, int gdsHashOverride, boolean useTableVersion,
      boolean intvMerge, boolean useCenter) {
    this (cust, gr.getPDSsection(), useTableVersion, intvMerge, useCenter, gdsHashOverride != 0 ? gdsHashOverride : gr.getGDS().hashCode());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Grib1Variable var2 = (Grib1Variable) o;
    if (gdsHash != var2.gdsHash)
      return false;
   // if (!gds.equals(var2.gds))
   //   return false;

    Grib1SectionProductDefinition pds2 = var2.pds;
    if (pds.getParameterNumber() != pds2.getParameterNumber())
      return false;
    if (pds.getLevelType() != pds2.getLevelType())
      return false;

    if (useTableVersion) {
      if (pds.getTableVersion() != pds2.getTableVersion())
        return false;
    }

    Grib1ParamTime ptime = cust.getParamTime(pds);
    Grib1ParamTime ptime2 = cust.getParamTime(pds2);
    if (ptime.isInterval() != ptime2.isInterval())
      return false;
    if (ptime.isInterval()) {
      if (!intvMerge) {
        if (ptime.getIntervalSize() != ptime2.getIntervalSize())
          return false;
      }
      if (ptime.getStatType() != ptime2.getStatType())
        return false;
    }

    if (useCenter && pds.getParameterNumber() > 127) {
      if (pds.getCenter() != pds2.getCenter())
        return false;
      return pds.getSubCenter() == pds2.getSubCenter();
    }

    return true;
  }

  @Override
  public int hashCode() { // could switch to using guava goodFastHash, if not storing (?)
    int result = 17; // Warning: a new random seed for these functions is chosen each time the Hashing class is loaded.
                     // Do not use this method if hash codes may escape the current process in any way, for example
                     // being sent over RPC, or saved to disk.

    result += result * 31 + pds.getParameterNumber();
    result += result * 31 + gdsHash;

    result += result * 31 + pds.getLevelType();

    if (useTableVersion)
      result += result * 31 + pds.getTableVersion();

    Grib1ParamTime ptime = cust.getParamTime(pds);
    if (ptime.isInterval()) {
      if (!intvMerge)
        result += result * 31 + ptime.getIntervalSize(); // create new variable for each interval size
      if (ptime.getStatType() != null)
        result += result * 31 + ptime.getStatType().ordinal(); // create new variable for each stat type
    }

    // if useCenter, and this uses any local tables, then we have to add the center id, and subcenter if present
    if (useCenter && pds.getParameterNumber() > 127) {
      result += result * 31 + pds.getCenter();
      if (pds.getSubCenter() > 0)
        result += result * 31 + pds.getSubCenter();
    }
    return result;
  }

  public String makeVariableName(GribConfig gribConfig) {
    return Grib1Iosp.makeVariableName(cust, gribConfig, pds);
  }
}
