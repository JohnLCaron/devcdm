/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grid.api;

import dev.ucdm.core.api.Group;

/** Static utilities for testing */
public class TestUtils {
  public static Group makeDummyGroup() {
    return Group.builder().setName("").build();
  }
}
