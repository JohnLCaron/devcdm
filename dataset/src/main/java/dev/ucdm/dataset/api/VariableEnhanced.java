/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.api;

import dev.ucdm.core.api.Group;

import org.jetbrains.annotations.Nullable;

/** Interface to an "enhanced Variable". */
public interface VariableEnhanced {

  /** Get the full name of this Variable, with Group names */
  String getFullName();

  /** Get the short name of this Variable, local to its parent Group. */
  String getShortName();

  /** A VariableDS usually wraps another Variable. */
  @Nullable
  dev.ucdm.core.api.Variable getOriginalVariable();

  /** The original name of the Variable (in case it was renamed in NcML). */
  @Nullable
  String getOriginalName();

  /** Get the description of the Variable, or null if none. */
  @Nullable
  String getDescription();

  /** Get the Unit String for the Variable, or null if none. */
  @Nullable
  String getUnitsString();

  /** Get the containing Group. */
  Group getParentGroup();
}
