package dev.cdm.dataset.spi;

import dev.cdm.core.api.CdmFile;
import dev.cdm.dataset.api.CdmDatasetCS;
import dev.cdm.dataset.internal.CoordSystemBuilderOld;

import org.jetbrains.annotations.Nullable;

/** A Service Provider of CoordSystemBuilder. */
public interface CoordSystemBuilderProvider {
  @Nullable
  String getConventionName();

  default boolean isMine(CdmFile ncfile) {
    return false; // if false, must have correct convention name.
  }

  CoordSystemBuilderOld open(CdmDatasetCS.Builder<?> datasetBuilder);
}
