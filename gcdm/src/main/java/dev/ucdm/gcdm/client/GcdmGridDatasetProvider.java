/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.gcdm.client;

import dev.ucdm.core.util.CancelTask;
import dev.ucdm.dataset.api.DatasetUrl;
import dev.ucdm.grid.api.*;

public class GcdmGridDatasetProvider implements GridDatasetProvider {
  @Override
  public String getProtocol() {
    return "gcdm";
  }

  @Override
  public boolean isOwnerOf(DatasetUrl url) {
    return url.getServiceType() == DatasetUrl.ServiceType.GCDM;
  }

  @Override
  public GridDataset open(String location, CancelTask cancelTask) {
    return GcdmGridDataset.builder().setRemoteURI(location).build(true);
  }

}
