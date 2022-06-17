package dev.cdm.dataset.spi;

import dev.cdm.core.api.CdmFile;
import dev.cdm.dataset.api.NetcdfDataset;
import dev.cdm.dataset.internal.CoordSystemBuilder;

import org.jetbrains.annotations.Nullable;

/** A Service Provider of CoordSystemBuilder. */
public interface CoordSystemBuilderProvider {
  @Nullable
  String getConventionName();

  default boolean isMine(CdmFile ncfile) {
    return false; // if false, must have correct convention name.
  }

  CoordSystemBuilder open(NetcdfDataset.Builder<?> datasetBuilder);
}
