/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.inventory;

public enum CollectionUpdate {
  always, // force new index creation, scanning files and directories as needed
  test, // test if top index is up-to-date, and if collection has changed
  nocheck, // dont check date, just use it if index exists
  never,   // only use existing, fail if doesnt already exist
}
