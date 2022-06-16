/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.dataset.ncml;

import dev.cdm.dataset.api.DatasetUrl;
import org.jdom2.Element;
import dev.cdm.core.api.CdmFile;
import dev.cdm.core.util.CancelTask;

import java.io.IOException;

class NcmlElementReader extends NcmlReader {
  private final Element netcdfElem;
  private final String ncmlLocation;
  private final String location;

  NcmlElementReader(String ncmlLocation, String location, Element netcdfElem) {
    this.ncmlLocation = ncmlLocation;
    this.location = location;
    this.netcdfElem = netcdfElem;
  }

  public CdmFile open(DatasetUrl cacheName, int bufferSize, CancelTask cancelTask, Object spiObject)
      throws IOException {
    CdmFile.Builder<?> result = readNcml(ncmlLocation, location, netcdfElem, cancelTask);
    result.setLocation(cacheName.getTrueurl());
    return result.build();
  }
}
