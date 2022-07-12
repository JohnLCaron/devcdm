/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.hdf5;

import static com.google.common.truth.Truth.assertThat;
import org.junit.jupiter.api.Test;
import dev.ucdm.array.ArrayType;
import dev.ucdm.core.api.EnumTypedef;
import dev.ucdm.core.api.CdmFile;
import dev.ucdm.core.api.CdmFiles;
import dev.ucdm.core.api.Variable;

/** Test handling of enums in hdf5 / netcdf 4 files. */
public class TestEnumTypedef {

  @Test
  public void problem() throws Exception {
    try (CdmFile ncfile = CdmFiles.open("src/test/data/netcdf4/test_atomic_types.nc")) {
      Variable primaryCloud = ncfile.findVariable("primary_cloud");
      assertThat((Object) primaryCloud).isNotNull();
      assertThat(primaryCloud.getArrayType().isEnum());
      assertThat(primaryCloud.getArrayType()).isEqualTo(ArrayType.ENUM1);
      assertThat(primaryCloud.getEnumTypedef()).isNotNull();
      EnumTypedef typedef = primaryCloud.getEnumTypedef();
      assertThat(typedef).isNotNull();
      // TODO disable this until we have a fix see Issue #126
      // assertThat(typedef.getShortName()).isEqualTo("cloud_class_t");
    }
  }

}
