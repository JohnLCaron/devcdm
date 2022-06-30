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
import dev.cdm.dataset.api.CdmDatasetCS;
import dev.cdm.dataset.spi.CoordSystemBuilderProvider;
import dev.cdm.dataset.internal.CoordSystemBuilderOld;
import dev.cdm.core.util.CancelTask;

/** FslWindProfiler netcdf files - identify coordinates */
public class FslWindProfiler extends CoordSystemBuilderOld {
  private static final String CONVENTION_NAME = "FslWindProfiler";

  public static class Factory implements CoordSystemBuilderProvider {
    @Override
    public String getConventionName() {
      return CONVENTION_NAME;
    }

    @Override
    public boolean isMine(CdmFile ncfile) {
      String title = ncfile.getRootGroup().attributes().findAttributeString("title", null);
      return title != null && (title.startsWith("WPDN data"));
    }

    @Override
    public CoordSystemBuilderOld open(CdmDatasetCS.Builder<?> datasetBuilder) {
      return new FslWindProfiler(datasetBuilder);
    }
  }

  private FslWindProfiler(CdmDatasetCS.Builder<?> datasetBuilder) {
    super(datasetBuilder);
    this.conventionName = CONVENTION_NAME;
  }

  @Override
  public void augmentDataset(CancelTask cancelTask) {
    for (Variable.Builder<?> v : rootGroup.vbuilders) {
      switch (v.shortName) {
        case "staName":
          v.addAttribute(new Attribute("standard_name", "station_name"));
          break;
        case "staLat":
          v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
          break;
        case "staLon":
          v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
          break;
        case "staElev":
        case "levels":
          v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Height.toString()));
          break;
        case "timeObs":
          v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
          break;
      }
    }
  }

}
