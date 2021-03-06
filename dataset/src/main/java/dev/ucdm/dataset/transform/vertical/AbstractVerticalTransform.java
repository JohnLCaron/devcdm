/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.transform.vertical;

import dev.ucdm.array.Array;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.Arrays;
import dev.ucdm.array.InvalidRangeException;
import dev.ucdm.array.Section;
import dev.ucdm.core.api.AttributeContainer;
import dev.ucdm.core.api.Variable;
import dev.ucdm.dataset.api.CdmDataset;
import dev.ucdm.core.util.StringUtil2;

import org.jetbrains.annotations.Nullable;
import dev.ucdm.array.Immutable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/** Superclass for implementations of a VerticalTransform. */
@Immutable
abstract class AbstractVerticalTransform implements VerticalTransform {

  static Array<Number> readArray(CdmDataset ncd, String varName) throws IOException {
    Variable v = ncd.findVariable(varName);
    if (v == null) {
      throw new IllegalArgumentException(varName);
    }
    return (Array<Number>) v.readArray();
  }

  static double readScalarDouble(CdmDataset ncd, String varName) throws IOException {
    Variable v = ncd.findVariable(varName);
    if (v == null) {
      throw new IllegalArgumentException(varName);
    }
    Array<Number> data = (Array<Number>) v.readArray();
    return data.getScalar().doubleValue();
  }

  static String getUnits(CdmDataset ncd, String varName) {
    Variable v = ncd.findVariable(varName);
    if (v == null) {
      throw new IllegalArgumentException(varName);
    }
    return v.getUnitsString();
  }

  static int getRank(CdmDataset ncd, String varName) {
    Variable v = ncd.findVariable(varName);
    if (v == null) {
      throw new IllegalArgumentException(varName);
    }
    return v.getRank();
  }

  static Array<Number> readArray(CdmDataset ncd, String varName, int timeIdx)
      throws IOException, InvalidRangeException {
    Variable v = ncd.findVariable(varName);
    if (v == null) {
      throw new IllegalArgumentException(varName);
    }
    int[] shape = v.getShape();
    int[] origin = new int[v.getRank()];

    // assume the time dimension exists and is the outer dimension
    shape[0] = 1;
    origin[0] = timeIdx;

    Array<Number> data = (Array<Number>) v.readArray(new Section(origin, shape));
    return Arrays.reduce(data, 0);
  }

  //////////////////////////////////////////////////////////

  protected final CdmDataset ds;
  protected final String name; // unique in the dataset
  protected final String units;

  AbstractVerticalTransform(CdmDataset ds, String name, String units) {
    this.ds = ds;
    this.name = name;
    this.units = units;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  @Nullable
  public String getUnitString() {
    return units;
  }

  static String getFormula(AttributeContainer ctv, Formatter errlog) {
    String formula = ctv.findAttributeString("formula_terms", null);
    if (null == formula) {
      errlog.format("CoordTransBuilder %s: needs attribute 'formula_terms' on %s%n", ctv.getName(), ctv.getName());
      return null;
    }
    return formula;
  }

  static List<String> parseFormula(String formula_terms, String termString, Formatter errlog) {
    String[] formulaTerms = formula_terms.split("[\\s:]+"); // split on 1 or more whitespace or ':'
    Iterable<String> terms = StringUtil2.split(termString); // split on 1 or more whitespace
    List<String> values = new ArrayList<>();

    for (String term : terms) {
      for (int j = 0; j < formulaTerms.length; j += 2) { // look at every other formula term
        if (term.equals(formulaTerms[j])) { // if it matches
          values.add(formulaTerms[j + 1]); // next term is the value
          break;
        }
      }
    }
    return values;
  }

}

