package dev.cdm.dataset.internal;

import dev.cdm.dataset.api.CdmDataset.Enhance;
import dev.cdm.dataset.api.CdmDatasetCS;

import java.io.IOException;
import java.util.Set;

/**
 * Helper class to enhance NetcdfDataset with scale/offset/missing and coordinate systems.
 */
public class DatasetCSEnhancer extends DatasetEnhancer {
  private final CdmDatasetCS.Builder<?> dscsBuilder;

  public DatasetCSEnhancer(CdmDatasetCS.Builder<?> ds, Set<Enhance> wantEnhance) {
    super(ds, wantEnhance);
    this.dscsBuilder = ds;
  }

  public CdmDatasetCS.Builder<?> enhance() throws IOException {
    // CoordSystemBuilder may enhance dataset: add new variables, attributes, etc
    CoordSystemBuilder coordSysBuilder = CoordSystemFactory.factory(dscsBuilder, null);
    coordSysBuilder.augmentDataset(null);
    dscsBuilder.setConventionUsed(coordSysBuilder.getConventionUsed());

    // regular enhancements
    if (!this.wantEnhance.isEmpty()) {
      super.enhance();
    }

    // add the coordinate systems to dscsBuilder
    coordSysBuilder.buildCoordinateSystems();

    return dscsBuilder;
  }
}
