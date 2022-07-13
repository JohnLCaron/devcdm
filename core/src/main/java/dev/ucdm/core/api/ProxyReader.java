/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.api;

import dev.ucdm.array.Array;
import dev.ucdm.core.util.CancelTask;

import java.io.IOException;

/** Reader of the data for a Variable. */
public interface ProxyReader {

  /** Read all the data for a Variable, returning Array. */
  Array<?> proxyReadArray(Variable client, CancelTask cancelTask) throws IOException;

  /** Read a section of the data for a Variable, returning Array. */
  Array<?> proxyReadArray(Variable client, dev.ucdm.array.Section section, CancelTask cancelTask)
      throws IOException, dev.ucdm.array.InvalidRangeException;

}
