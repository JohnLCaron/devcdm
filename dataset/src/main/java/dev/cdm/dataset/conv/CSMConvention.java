/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.conv;

import dev.cdm.core.api.Attribute;
import dev.cdm.core.api.Variable;
import dev.cdm.core.constants.AxisType;
import dev.cdm.core.constants.CDM;
import dev.cdm.core.constants.CF;
import dev.cdm.core.constants._Coordinate;
import dev.cdm.dataset.api.CdmDataset;
import dev.cdm.dataset.api.VariableDS;
import dev.cdm.dataset.spi.CoordSystemBuilderProvider;
import dev.cdm.dataset.internal.CoordSystemBuilder;
import dev.cdm.core.util.CancelTask;
import dev.cdm.dataset.transform.vertical.CsmHybridSigmaBuilder;

import java.io.IOException;

/**
 * CSM-1 Convention.
 * Obsolete, do not use for new data.
 */
class CSMConvention extends CoardsConventions {
  private static final String CONVENTION_NAME = "NCAR-CSM";

  CSMConvention(CdmDataset.Builder<?> datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  @Override
  protected void augmentDataset(CancelTask cancelTask) throws IOException {
    for (Variable.Builder<?> vb : rootGroup.vbuilders) {
      if (!(vb instanceof VariableDS.Builder<?>)) {
        continue;
      }
      VariableDS.Builder<?> vds = (VariableDS.Builder<?>) vb;
      String unit = vds.getUnits();
      if (unit != null && (unit.equalsIgnoreCase("hybrid_sigma_pressure") || unit.equalsIgnoreCase("sigma_level"))) {
        // both a coordinate axis and transform
        vds.addAttribute(new Attribute(CDM.TRANSFORM_NAME, CsmHybridSigmaBuilder.transform_name));
        vds.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.GeoZ.toString()));
        vds.addAttribute(new Attribute(_Coordinate.TransformType, CDM.Vertical));
        vds.addAttribute(new Attribute(_Coordinate.Axes, vds.getFullName()));
      }
    }
  }

  @Override
  protected void identifyCoordinateAxes() {
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
  }

  public static class Factory implements CoordSystemBuilderProvider {

    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public CoordSystemBuilder open(CdmDataset.Builder<?> datasetBuilder) {
      return new CSMConvention(datasetBuilder);
    }
  }

}
