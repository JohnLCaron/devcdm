/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.transform.vertical;

import com.google.common.base.Preconditions;
import dev.cdm.array.Array;
import dev.cdm.array.InvalidRangeException;
import dev.cdm.core.api.AttributeContainer;
import dev.cdm.core.constants.CF;
import dev.cdm.dataset.api.CoordinateSystem;
import dev.cdm.dataset.api.NetcdfDataset;

import dev.cdm.array.Immutable;
import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

/**
 * Create a Vertical Transform from a variable in the dataset.
 */
@Immutable
public class ExistingFieldVerticalTransform extends AbstractVerticalTransform {
  public static final String transform_name = "explicit_field";
  public static final String existingDataField = "existingDataField";

  public static Optional<VerticalTransform> create(NetcdfDataset ds, AttributeContainer params, Formatter errlog) {

    String existingField = params.findAttributeString(existingDataField, null);
    if (existingField == null) {
      errlog.format("ExistingFieldVerticalTransform %s: existingField attribute not present%n", params.getName());
      return Optional.empty();
    }

    int rank = getRank(ds, existingField);
    if (rank != 3 && rank != 4) {
      errlog.format("ExistingFieldVerticalTransform %s: existingField has rank %d should be 3 or 4%n", params.getName(),
          rank);
      return Optional.empty();
    }
    String units = getUnits(ds, existingField);

    return Optional.of(new ExistingFieldVerticalTransform(ds, params.getName(), units, existingField, rank));
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////
  private final String existingField;
  private final int rank;

  private ExistingFieldVerticalTransform(NetcdfDataset ds, String ctvName, String units, String existingField,
      int rank) {
    super(ds, CF.ocean_sigma_coordinate, ctvName, units);

    this.existingField = existingField;
    this.rank = rank;
  }

  @Override
  public Array<Number> getCoordinateArray3D(int timeIndex) throws IOException, InvalidRangeException {
    Array<Number> result = (rank == 4) ? readArray(ds, existingField, timeIndex) : readArray(ds, existingField);
    Preconditions.checkArgument(result.getRank() == 3);
    return result;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////

  public static class Builder implements VerticalTransform.Builder {
    public Optional<VerticalTransform> create(NetcdfDataset ds, CoordinateSystem csys, AttributeContainer params,
                                              Formatter errlog) {
      return ExistingFieldVerticalTransform.create(ds, params, errlog);
    }
  }
}

