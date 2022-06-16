/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.conv;

import dev.cdm.core.api.Attribute;
import dev.cdm.core.api.CdmFile;
import dev.cdm.core.api.Variable;
import dev.cdm.core.constants.AxisType;
import dev.cdm.core.constants._Coordinate;
import dev.cdm.dataset.api.NetcdfDataset;
import dev.cdm.dataset.spi.CoordSystemBuilderProvider;
import dev.cdm.dataset.internal.CoordSystemBuilder;
import dev.cdm.core.util.CancelTask;

import java.util.StringTokenizer;

/** MADIS Station Convention. */
public class MADISStation extends CoordSystemBuilder {
  private static final String CONVENTION_NAME = "MADIS_Station_1.0";

  public static class Factory implements CoordSystemBuilderProvider {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public boolean isMine(CdmFile ncfile) {
      String s = ncfile.getRootGroup().attributes().findAttributeString("Conventions", "none");
      return s.startsWith("MADIS");
    }

    @Override
    public CoordSystemBuilder open(NetcdfDataset.Builder<?> datasetBuilder) {
      return new MADISStation(datasetBuilder);
    }
  }

  /////////////////////////////////////////////////////////////////

  private MADISStation(NetcdfDataset.Builder<?> datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  @Override
  public void augmentDataset(CancelTask cancelTask) {
    String timeVars = rootGroup.getAttributeContainer().findAttributeString("timeVariables", "");
    StringTokenizer stoker = new StringTokenizer(timeVars, ", ");
    while (stoker.hasMoreTokens()) {
      String vname = stoker.nextToken();
      rootGroup.findVariableLocal(vname)
          .ifPresent(v -> v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString())));
    }

    String locVars = rootGroup.getAttributeContainer().findAttributeString("stationLocationVariables", "");
    stoker = new StringTokenizer(locVars, ", ");
    int count = 0;
    while (stoker.hasMoreTokens()) {
      String vname = stoker.nextToken();
      if (rootGroup.findVariableLocal(vname).isPresent()) {
        Variable.Builder<?> v = rootGroup.findVariableLocal(vname).get();
        AxisType atype = count == 0 ? AxisType.Lat : count == 1 ? AxisType.Lon : AxisType.Height;
        v.addAttribute(new Attribute(_Coordinate.AxisType, atype.toString()));
      } else {
        parseInfo.format(" cant find time variable %s%n", vname);
      }
      count++;
    }
  }

}
