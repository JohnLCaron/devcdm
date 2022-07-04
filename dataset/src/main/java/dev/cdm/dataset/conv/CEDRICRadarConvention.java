/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.dataset.conv;

import dev.cdm.core.api.Attribute;
import dev.cdm.core.api.Dimension;
import dev.cdm.core.api.CdmFile;
import dev.cdm.core.api.Variable;
import dev.cdm.dataset.api.CdmDatasetCS;
import dev.cdm.dataset.api.VariableDS;
import dev.cdm.dataset.ncml.NcmlReader;
import dev.cdm.dataset.spi.CoordSystemBuilderProvider;
import dev.cdm.dataset.internal.CoordSystemBuilderOld;
import dev.cdm.dataset.internal.CoordSystemFactory;
import dev.cdm.core.util.CancelTask;

import java.io.IOException;

public class CEDRICRadarConvention extends CF1Convention {
  private static final String CONVENTION_NAME = "CEDRICRadar";

  public static class Factory implements CoordSystemBuilderProvider {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public boolean isMine(CdmFile ncfile) {
      return (ncfile.findDimension("cedric_general_scaling_factor") != null) &&
              (ncfile.findVariable("cedric_run_date")  != null) &&
              (ncfile.findVariable("sensor_latitude")  != null) &&
              (ncfile.findVariable("sensor_longitude")  != null) &&
              (ncfile.findVariable("Projection") != null);
    }

    @Override
    public CoordSystemBuilderOld open(CdmDatasetCS.Builder<?> datasetBuilder) {
      return new CEDRICRadarConvention(datasetBuilder);
    }
  }

  CEDRICRadarConvention(CdmDatasetCS.Builder<?> datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  @Override
  protected void augmentDataset(CancelTask cancelTask) throws IOException {
    NcmlReader.wrapNcmlResource(datasetBuilder, CoordSystemFactory.resourcesDir + "CEDRICRadar.ncml", cancelTask);

    VariableDS.Builder<?> lat = (VariableDS.Builder<?>) rootGroup.findVariableLocal("sensor_latitude")
        .orElseThrow(() -> new IllegalStateException("Must have sensor_latitude variable"));
    VariableDS.Builder<?> lon = (VariableDS.Builder<?>) rootGroup.findVariableLocal("sensor_longitude")
        .orElseThrow(() -> new IllegalStateException("Must have sensor_longitude variable"));
    float latv = ((Number) lat.orgVar.readArray().getScalar()).floatValue();
    float lonv = ((Number) lon.orgVar.readArray().getScalar()).floatValue();

    VariableDS.Builder<?> pv = (VariableDS.Builder<?>) rootGroup.findVariableLocal("Projection")
        .orElseThrow(() -> new IllegalStateException("Must have Projection variable"));
    pv.addAttribute(new Attribute("longitude_of_projection_origin", lonv));
    pv.addAttribute(new Attribute("latitude_of_projection_origin", latv));

    super.augmentDataset(cancelTask);
  }

}


