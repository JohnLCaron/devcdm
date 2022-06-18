/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.core.http;

import dev.cdm.core.http.InMemoryRafHttpProvider;
import org.junit.jupiter.api.Test;
import dev.cdm.core.io.InMemoryRandomAccessFile;
import dev.cdm.core.io.RandomAccessFile;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link dev.cdm.core.http.InMemoryRafHttpProvider} */
public class TestInMemoryRafHttpProvider {
  // Some random file on the TDS
  private final String baseHttpLocation =
      "thredds.ucar.edu/thredds/fileServer/casestudies/irma/text/upper_air/upper_air_20170911_2300.txt";
  private final String httpsLocation = "https://" + baseHttpLocation;

  @Test
  public void testInMemoryHttpProvider() throws IOException {
    System.out.printf("testInMemoryHttpProvider Open %s%n", httpsLocation);
    InMemoryRafHttpProvider provider = new InMemoryRafHttpProvider();
    try (RandomAccessFile rafh = provider.open(httpsLocation)) {
      assertThat(rafh).isInstanceOf(InMemoryRandomAccessFile.class);
      assertThat(rafh.getLocation()).isEqualTo(httpsLocation);
      assertThat(rafh.getLastModified()).isEqualTo(0);
      assertThat(rafh.length()).isEqualTo(18351);

      // read a couple of random bytes
      byte[] buff = new byte[2];
      rafh.seek(42L);
      assertThat(rafh.read(buff)).isEqualTo(2);
      assertThat(buff[0]).isEqualTo(32);
      assertThat(buff[1]).isEqualTo(55);
    }
  }
}
