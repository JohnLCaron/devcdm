/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.internal;

import dev.cdm.array.Array;
import dev.cdm.array.ArrayType;
import dev.cdm.array.Arrays;
import dev.cdm.dataset.api.NetcdfDataset.Enhance;
import dev.cdm.dataset.api.VariableDS;

import java.util.Set;

/** Does enhancements to a VariableDS' data. */
public class DataEnhancer {
  private final VariableDS variableDS;
  private final ArrayType dataType;
  private final ArrayType orgDataType;
  private final EnhanceScaleMissingUnsigned scaleMissingUnsignedProxy;

  public DataEnhancer(VariableDS variableDS, EnhanceScaleMissingUnsigned scaleMissingUnsignedProxy) {
    this.variableDS = variableDS;
    this.dataType = variableDS.getArrayType();
    this.orgDataType = variableDS.getOriginalArrayType();
    this.scaleMissingUnsignedProxy = scaleMissingUnsignedProxy;
  }

  public Array<?> convertArray(Array<?> data, Set<Enhance> enhancements) {
    if (enhancements.contains(Enhance.ConvertEnums)
        && (dataType.isEnum() || (orgDataType != null && orgDataType.isEnum()))) {
      // Creates STRING data. As a result, we can return here, because the other conversions don't apply to STRING.
      return convertEnums(data);
    } else {
      // TODO: make this work for isVariableLength; i thought BUFR worked?
      if (variableDS.isVariableLength()) {
        return data;
      }
      return scaleMissingUnsignedProxy.convert(data, enhancements.contains(Enhance.ConvertUnsigned),
          enhancements.contains(Enhance.ApplyScaleOffset), enhancements.contains(Enhance.ConvertMissing));
    }
  }

  private Array<?> convertEnums(Array<?> values) {
    if (values.getArrayType() == ArrayType.STRING) {
      return values; // Nothing to do!
    }

    String[] sdata = new String[(int) values.getSize()];
    int count = 0;
    for (Number val : (Array<Number>) values) {
      String sval = variableDS.lookupEnumString(val.intValue());
      sdata[count++] = sval == null ? "unknown-" + val : sval;
    }
    return Arrays.factory(ArrayType.STRING, values.getShape(), sdata);
  }

}
