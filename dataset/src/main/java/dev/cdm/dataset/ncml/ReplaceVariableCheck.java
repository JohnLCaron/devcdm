/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.dataset.ncml;

import dev.cdm.core.api.Variable;

interface ReplaceVariableCheck {
  boolean replace(Variable v);
}
