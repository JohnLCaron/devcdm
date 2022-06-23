/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.gcdm.client;

import dev.cdm.core.api.CdmFile;
import dev.cdm.core.util.CancelTask;
import dev.cdm.dataset.api.DatasetUrl;
import dev.cdm.dataset.spi.CdmFileProvider;

public class GcdmCdmFileProvider implements CdmFileProvider {
  @Override
  public String getProtocol() {
    return "gcdm";
  }

  @Override
  public boolean isOwnerOf(DatasetUrl url) {
    return url.getServiceType() == DatasetUrl.ServiceType.GCDM;
  }

  @Override
  public CdmFile open(String location, CancelTask cancelTask) {
    return GcdmCdmFile.builder().setRemoteURI(location).build();
  }

}
