/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.core;

import dev.cdm.util.CancelTask;

import java.io.IOException;

/** Reader of the data for a Variable. */
public interface ProxyReader {

  /** Read all the data for a Variable, returning dev.cdm.array.Array. */
  dev.cdm.array.Array<?> proxyReadArray(Variable client, CancelTask cancelTask) throws IOException;

  /** Read a section of the data for a Variable, returning dev.cdm.array.Array. */
  dev.cdm.array.Array<?> proxyReadArray(Variable client, dev.cdm.array.Section section, CancelTask cancelTask)
      throws IOException, dev.cdm.array.InvalidRangeException;

}
