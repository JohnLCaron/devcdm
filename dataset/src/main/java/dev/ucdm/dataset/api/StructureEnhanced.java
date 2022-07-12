/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.api;

import dev.ucdm.core.api.Variable;

import org.jetbrains.annotations.Nullable;
import java.util.List;

/** Interface to an "enhanced Structure". */
public interface StructureEnhanced extends VariableEnhanced {
  /** Find the Variable member with the specified (short) name, or null if not found. */
  @Nullable
  Variable findVariable(String shortName);

  /** Get the variables contained directly in this Structure. */
  List<Variable> getVariables();
}
