/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.grib2.iosp;


import dev.ucdm.grib.grib2.record.Grib2Gds;
import dev.ucdm.grib.grib2.record.Grib2Pds;
import dev.ucdm.grib.grib2.record.Grib2Record;
import dev.ucdm.grib.grib2.table.Grib2Tables;

import java.util.Formatter;

/**
 * Used to group records into a CDM variable
 * Herein lies the semantics of variable object identity.
 * Read it and weep.
 */
public record Grib2Variable(Grib2Tables cust, int discipline, int center, int subcenter, Grib2Pds pds,
                           boolean intvMerge, boolean useGenType, int gdsHash) {

  /**
   * Used when building from gbx9
   *
   * @param cust customizer
   * @param gr the Grib record
   * @param gdsHashOverride can override the gdsHash, 0 for no override
   * @param intvMerge should intervals be merged? default true
   * @param useGenType should genProcessType be used in hash? default false
   */
  public Grib2Variable(Grib2Tables cust, Grib2Record gr, int gdsHashOverride, boolean intvMerge, boolean useGenType) {
    this(cust, gr.getDiscipline(), gr.getId().getCenter_id(), gr.getId().getSubcenter_id(), gr.getPDS(),
        intvMerge, useGenType, gdsHashOverride != 0 ? gdsHashOverride : gr.getGDS().hashCode());
  }

  /**
   * Used when building from ncx
   */
  public Grib2Variable(Grib2Tables cust, int discipline, int center, int subcenter, Grib2Gds gds, Grib2Pds pds,
      boolean intvMerge, boolean useGenType) {
    this(cust, discipline, center, subcenter, pds, intvMerge, useGenType, gds.hashCode());
    // this.gdsHash = gds.hashCode(); // ok because no overridden gds hashCodes have made it into the ncx
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Grib2Variable var2 = (Grib2Variable) o;
    if (gdsHash != var2.gdsHash)
      return false;

    // LOOK can we get away with only using the gdsHash ??
    //if (!gds.equals(var2.gds))
    //  return false;

    Grib2Pds pds2 = var2.pds;

    if (pds.getParameterNumber() != pds2.getParameterNumber())
      return false;
    if (pds.getParameterCategory() != pds2.getParameterCategory())
      return false;
    if (pds.getTemplateNumber() != pds2.getTemplateNumber())
      return false;
    if (discipline != var2.discipline)
      return false;

    if (Grib2Utils.isLayer(pds) != Grib2Utils.isLayer(pds2))
      return false;
    if (pds.getLevelType1() != pds2.getLevelType1())
      return false;

    if (pds.isTimeInterval() != pds2.isTimeInterval())
      return false;
    if (pds.isTimeInterval()) {
      if (!intvMerge) {
        // TODO only used to decide on variable identity, so why make into a hour, just compare in millis ??
        double size = cust.getForecastTimeIntervalSizeInHours(pds); // only used to decide on variable identity
        double size2 = cust.getForecastTimeIntervalSizeInHours(pds2);
        if (size != size2)
          return false;
      }
      if (pds.getStatisticalProcessType() != pds2.getStatisticalProcessType())
        return false;
    }

    if (pds.isSpatialInterval() != pds2.isSpatialInterval())
      return false;
    if (pds.isSpatialInterval()) {
      Grib2Pds.PdsSpatialInterval pdsSpatial = (Grib2Pds.PdsSpatialInterval) pds;
      if (pdsSpatial.getSpatialStatisticalProcessType() != pdsSpatial.getSpatialStatisticalProcessType())
        return false;
    }

    int ensDerivedType = -1;
    if (pds.isEnsembleDerived() != pds2.isEnsembleDerived())
      return false;
    if (pds.isEnsembleDerived()) {
      Grib2Pds.PdsEnsembleDerived pdsDerived = (Grib2Pds.PdsEnsembleDerived) pds;
      Grib2Pds.PdsEnsembleDerived pdsDerived2 = (Grib2Pds.PdsEnsembleDerived) pds2;
      if (pdsDerived.getDerivedForecastType() != pdsDerived2.getDerivedForecastType())
        return false;
      ensDerivedType = pdsDerived.getDerivedForecastType(); // derived type (table 4.7)

    } else {
      if (pds.isEnsemble() != pds2.isEnsemble())
        return false;
    }

    int probType = -1;
    if (pds.isProbability() != pds2.isProbability())
      return false;
    if (pds.isProbability()) {
      Grib2Pds.PdsProbability pdsProb = (Grib2Pds.PdsProbability) pds;
      Grib2Pds.PdsProbability pdsProb2 = (Grib2Pds.PdsProbability) pds2;
      if (pdsProb.getProbabilityHashcode() != pdsProb2.getProbabilityHashcode())
        return false;
      probType = pdsProb.getProbabilityType();
    }

    if (pds.isPercentile() != pds2.isPercentile())
      return false;
    if (pds.isPercentile()) {
      Grib2Pds.PdsPercentile pdsPctl = (Grib2Pds.PdsPercentile) pds;
      Grib2Pds.PdsPercentile pdsPctl2 = (Grib2Pds.PdsPercentile) pds2;
      if (pdsPctl.getPercentileValue() != pdsPctl2.getPercentileValue())
        return false;
    }

    // if this uses any local tables, then we have to add the center id, and subcenter if present
    if ((pds2.getParameterCategory() > 191) || (pds2.getParameterNumber() > 191) || (pds2.getLevelType1() > 191)
        || (pds2.isTimeInterval() && pds2.getStatisticalProcessType() > 191) || (ensDerivedType > 191)
        || (probType > 191)) {

      if (center != var2.center)
        return false;
      if (subcenter != var2.subcenter)
        return false;
    }

    // always use the GenProcessType when "error" (6 or 7) 2/8/2012; added to equals 2/7/2016
    int genType = pds.getGenProcessType();
    int genType2 = pds2.getGenProcessType();
    boolean error = (genType == 6 || genType == 7);
    boolean error2 = (genType2 == 6 || genType2 == 7);
    if (error != error2)
      return false;
    return !useGenType || (genType == genType2);
  }


  @Override
  public int hashCode() {
    int result = 17;

    result += result * 31 + discipline;
    result += result * 31 + pds.getLevelType1();
    if (Grib2Utils.isLayer(pds))
      result += result * 31 + 1;

    result += result * 31 + this.gdsHash; // the horizontal grid

    result += result * 31 + pds.getParameterCategory();
    result += result * 31 + pds.getTemplateNumber();

    if (pds.isTimeInterval()) {
      if (!intvMerge) {
        // TODO only used to decide on variable identity, so why make into a hour, just compare in millis ??
        // TODO using an Hour here, but can we make this configurable
        // if you change the hashCode, I think you mess up the existing indices ??
        double size = cust.getForecastTimeIntervalSizeInHours(pds);
        result += result * (int) (31 + (1000 * size)); // create new variable for each interval size - default not
      }
      result += result * 31 + pds.getStatisticalProcessType(); // create new variable for each stat type
    }

    if (pds.isSpatialInterval()) {
      Grib2Pds.PdsSpatialInterval pdsSpatial = (Grib2Pds.PdsSpatialInterval) pds;
      result += result * 31 + pdsSpatial.getSpatialStatisticalProcessType(); // template 15
    }

    result += result * 31 + pds.getParameterNumber();

    int ensDerivedType = -1;
    if (pds.isEnsembleDerived()) { // a derived ensemble must have a derivedForecastType
      Grib2Pds.PdsEnsembleDerived pdsDerived = (Grib2Pds.PdsEnsembleDerived) pds;
      ensDerivedType = pdsDerived.getDerivedForecastType(); // derived type (table 4.7)
      result += result * 31 + ensDerivedType;

    } else if (pds.isEnsemble()) {
      result += result * 31 + 1;
    }

    // each probability interval generates a separate variable; could be a dimension instead
    int probType = -1;
    if (pds.isProbability()) {
      Grib2Pds.PdsProbability pdsProb = (Grib2Pds.PdsProbability) pds;
      probType = pdsProb.getProbabilityType();
      result += result * 31 + pdsProb.getProbabilityHashcode();
    }

    if (pds.isPercentile()) {
      Grib2Pds.PdsPercentile pdsPerc = (Grib2Pds.PdsPercentile) pds;
      result += result * 31 + pdsPerc.getPercentileValue();
    }

    // if this uses any local tables, then we have to add the center id, and subcenter if present
    if ((pds.getParameterCategory() > 191) || (pds.getParameterNumber() > 191) || (pds.getLevelType1() > 191)
        || (pds.isTimeInterval() && pds.getStatisticalProcessType() > 191) || (ensDerivedType > 191)
        || (probType > 191)) {
      result += result * 31 + center;
      if (subcenter > 0)
        result += result * 31 + subcenter;
    }

    // always use the GenProcessType when "error" (6 or 7) 2/8/2012
    int genType = pds.getGenProcessType();
    if (genType == 6 || genType == 7 || (useGenType && genType > 0)) {
      result += result * 31 + genType;
    }

    return result;
  }

  @Override
  public String toString() {
    try (Formatter sb = new Formatter()) {
      sb.format("Grib2Variable {%d-%d-%d", discipline, pds.getParameterCategory(), pds.getParameterNumber());
      sb.format(", levelType=%d", pds.getLevelType1());
      sb.format(", intvType=%d", pds.getStatisticalProcessType());
      sb.format(" hashCode=%d}", hashCode());
      return sb.toString();
    }
  }
}
