package dev.cdm.dataset.internal;

import dev.cdm.dataset.api.CdmDataset.Enhance;
import dev.cdm.dataset.api.CdmDatasetCS;

import java.io.IOException;
import java.util.Set;

/**
 * Helper class to enhance NetcdfDataset with scale/offset/missing and coordinate systems.
 * Uses old CoordSystemBuilder
 */
public class DatasetCSEnhancer extends DatasetEnhancer {
  private final CdmDatasetCS.Builder<?> dsBuilder;

  public DatasetCSEnhancer(CdmDatasetCS.Builder<?> ds, Set<Enhance> wantEnhance) {
    super(ds, wantEnhance);
    this.dsBuilder = ds;
  }

  public CdmDatasetCS.Builder<?> enhance() throws IOException {
    // CoordSystemBuilder may enhance dataset: add new variables, attributes, etc
    CoordSystemBuilderOld coordSysBuilder = CoordSystemFactory.factory(dsBuilder, null);
    coordSysBuilder.augmentDataset(null);
    dsBuilder.setConventionUsed(coordSysBuilder.getConventionUsed());

    // regular enhancements
    if (!this.wantEnhance.isEmpty()) {
      super.enhance();
    }

    // add the coordinate systems to dscsBuilder
    coordSysBuilder.buildCoordinateSystems();

    CoordinatesHelper.Builder coordsOld = coordSysBuilder.coords;
    dsBuilder.coordsOld = coordsOld;

    /* remove any coordinate axes in vbuilder
    for (var vb : coordsOld.coordAxes) {
      if (dsBuilder.rootGroup.removeVariable(vb.getFullName())) {
        System.out.printf("remove %s == %s%n", vb.getFullName(), vb.getClass().getName());
      }
    } */

    return dsBuilder;
  }
}
