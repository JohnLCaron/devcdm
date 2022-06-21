/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.ncml;

import dev.cdm.dataset.api.TestCdmDatasets;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import dev.cdm.array.Array;
import dev.cdm.core.api.CdmFile;
import dev.cdm.core.api.Structure;
import dev.cdm.core.api.Variable;
import dev.cdm.dataset.api.DatasetUrl;
import dev.cdm.dataset.api.CdmDatasets;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class TestCachedNcmlData {

  @Test
  public void testCachedData() throws IOException {

    try (CdmFile ncd = CdmDatasets.openFile(TestCdmDatasets.datasetLocalNcmlDir + "point/profileMultidim.ncml", null)) {
      Variable v = ncd.findVariable("data");
      assertThat(v).isNotNull();
      Array<?> data = v.readArray();
      assertThat(data.getSize()).isEqualTo(50);
    }
  }

  @Test
  @Ignore("doesnt work because CdmFileProvider cant pass in IospMessage")
  public void testCachedDataWithStructure() throws IOException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(TestCdmDatasets.datasetLocalNcmlDir + "point/profileMultidim.ncml");

    try (CdmFile ncd = CdmDatasets.openFile(durl, -1, null, CdmFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      Variable s = ncd.findVariable("record");
      assertThat(s).isNotNull();
      assertThat(s).isInstanceOf(Structure.class);
      assertThat(s.getSize()).isEqualTo(5);

      Array<?> data = s.readArray();
      assertThat(data.getSize()).isEqualTo(5);
    }
  }


}
