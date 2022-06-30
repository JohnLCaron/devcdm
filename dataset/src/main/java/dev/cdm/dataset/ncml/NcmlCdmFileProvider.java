/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.dataset.ncml;

import dev.cdm.core.api.CdmFile;
import dev.cdm.dataset.api.CdmDataset;
import dev.cdm.dataset.api.DatasetUrl;
import dev.cdm.dataset.spi.CdmFileProvider;
import dev.cdm.core.util.CancelTask;

import java.io.IOException;

/** Provider of CdmFile */
public class NcmlCdmFileProvider implements CdmFileProvider {

  @Override
  public String getProtocol() {
    return "ncml";
  }

  @Override
  public boolean isOwnerOf(DatasetUrl durl) {
    return durl.getServiceType() == DatasetUrl.ServiceType.NCML;
  }

  @Override
  public CdmFile open(String location, CancelTask cancelTask) throws IOException {
    CdmDataset.Builder<?> builder =  NcmlReader.readNcml(location, null, cancelTask);
    return builder.build();
  }

  @Override
  public boolean isOwnerOf(String location) {
    return location.startsWith("ncml:") || location.endsWith(".ncml");
  }

}
