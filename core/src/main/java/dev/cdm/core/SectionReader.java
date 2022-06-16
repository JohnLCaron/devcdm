/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.core;

import com.google.common.base.Preconditions;
import dev.cdm.array.Array;
import dev.cdm.array.InvalidRangeException;
import dev.cdm.array.Section;
import dev.cdm.util.CancelTask;

import javax.annotation.concurrent.Immutable;
import java.io.IOException;

/**
 * A ProxyReader for logical sections of a Variable.
 * 
 * {@link Variable#section(Section)}
 */
@Immutable
class SectionReader implements ProxyReader {
  private final Section orgSection; // section of the original
  private final Variable orgClient;

  /**
   * Reads logical sections of orgClient.
   * 
   * @param section of orgClient, will be filled if needed.
   */
  SectionReader(Variable orgClient, Section section) throws InvalidRangeException {
    Section filled = section.fill(orgClient.getShape());
    Preconditions.checkArgument(filled.checkInRange(orgClient.getShape()) == null);
    this.orgClient = orgClient;
    this.orgSection = filled;
  }

  @Override
  public Array<?> proxyReadArray(Variable client, CancelTask cancelTask) throws IOException {
    try {
      return orgClient._read(orgSection);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Array<?> proxyReadArray(Variable client, Section section, CancelTask cancelTask)
      throws IOException, InvalidRangeException {
    Section want = orgSection.compose(section);
    return orgClient._read(want);
  }

}
