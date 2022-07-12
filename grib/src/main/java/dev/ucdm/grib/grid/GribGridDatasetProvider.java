/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grid;

import dev.ucdm.core.util.CancelTask;
import dev.ucdm.dataset.api.DatasetUrl;
import dev.ucdm.grid.api.GridDataset;
import dev.ucdm.grid.api.GridDatasetProvider;

import java.io.IOException;
import java.util.Formatter;

public class GribGridDatasetProvider implements GridDatasetProvider {
  @Override
  public String getProtocol() {
    return "grib";
  }

  @Override
  public boolean isOwnerOf(DatasetUrl url) {
    return url.getServiceType() == DatasetUrl.ServiceType.GCDM;
  }

  @Override
  public GridDataset open(String location, CancelTask cancelTask) {
    Formatter errlog = new Formatter();
    try {
      return GribGridDataset.open(location, errlog);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
