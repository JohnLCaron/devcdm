package dev.cdm.dataset.internal;

import com.google.common.base.Strings;
import dev.cdm.array.Array;
import dev.cdm.array.InvalidRangeException;
import dev.cdm.core.constants.AxisType;
import dev.cdm.dataset.api.CdmDatasetCS;
import dev.cdm.dataset.api.CdmDatasets;
import dev.cdm.dataset.api.CoordinateSystem;
import dev.cdm.dataset.api.SimpleUnit;
import dev.cdm.dataset.api.TestCdmDatasets;
import dev.cdm.dataset.api.VariableDS;
import dev.cdm.dataset.transform.vertical.AtmosHybridSigmaPressure;
import dev.cdm.dataset.transform.vertical.VerticalTransform;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

public class TestVertical {

  @Test
  public void testHybridSigmaPressure() throws Exception {
    String filename = TestCdmDatasets.gridTestDir + "transforms/HybridSigmaPressure.nc";
    open(filename, "T", AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit);
  }

  static boolean open(String filename, String gridName, Class<?> vtClass, SimpleUnit vunit)
          throws IOException, InvalidRangeException {
    System.out.printf("compare %s %s%n", filename, gridName);

    Formatter errlog = new Formatter();
    try (CdmDatasetCS ds = CdmDatasets.openDatasetCS(filename, true)) {
      assertThat(ds).isNotNull();

      VariableDS grid = (VariableDS) ds.findVariable(gridName);
      List<CoordinateSystem> csyss =  ds.makeCoordinateSystemsFor(grid);
      assertThat(csyss).hasSize(1);
      CoordinateSystem csys = csyss.get(0);

      assertThat(csys.getProjection()).isNotNull();
      assertThat(csys.findAxis(AxisType.GeoZ)).isNotNull();
    }
    return true;
  }
}
