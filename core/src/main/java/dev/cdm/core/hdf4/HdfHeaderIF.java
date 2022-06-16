/*
 * Copyright (c) 1998-2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.core.hdf4;

import dev.cdm.core.api.Group;
import dev.cdm.core.api.Variable;

import java.io.IOException;

/** Interface for HDF5 and HDF4 headers, needed by HdfEos. */
public interface HdfHeaderIF {

  Group.Builder getRootGroup();

  // Need to set the Vinfo on a Variable that may get added by HdsEos.
  void makeVinfoForDimensionMapVariable(Group.Builder parent, Variable.Builder<?> v);

  // Need to read the struct metadata before we have a Variable
  String readStructMetadata(Variable.Builder<?> structMetadataVar) throws IOException;

}
