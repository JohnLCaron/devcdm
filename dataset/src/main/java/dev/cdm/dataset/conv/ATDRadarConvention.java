/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.conv;

import dev.cdm.dataset.ncml.NcmlReader;
import dev.cdm.core.api.CdmFile;
import dev.cdm.dataset.api.CdmDataset;
import dev.cdm.dataset.spi.CoordSystemBuilderProvider;
import dev.cdm.dataset.internal.CoordSystemBuilder;
import dev.cdm.dataset.internal.CoordSystemFactory;
import dev.cdm.core.util.CancelTask;

import java.io.IOException;

/** ATD Radar file (ad hoc guesses). */
public class ATDRadarConvention extends CoordSystemBuilder {
  private static final String CONVENTION_NAME = "ATDRadar";

  ATDRadarConvention(CdmDataset.Builder<?> datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  @Override
  public void augmentDataset(CancelTask cancelTask) throws IOException {
    NcmlReader.wrapNcmlResource(datasetBuilder, CoordSystemFactory.resourcesDir + "ATDRadar.ncml", cancelTask);
  }

  public static class Factory implements CoordSystemBuilderProvider {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public boolean isMine(CdmFile ncfile) {
      // not really sure until we can examine more files
      String s = ncfile.getRootGroup().findAttributeString("sensor_name", "none");
      return s.equalsIgnoreCase("CRAFT/NEXRAD");
    }

    @Override
    public CoordSystemBuilder open(CdmDataset.Builder<?> datasetBuilder) {
      return new ATDRadarConvention(datasetBuilder);
    }
  }

}
