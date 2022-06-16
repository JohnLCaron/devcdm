package dev.cdm.dataset.spi;

import dev.cdm.core.api.CdmFile;
import dev.cdm.core.util.CancelTask;
import dev.cdm.dataset.api.DatasetUrl;

import java.io.IOException;

/** A Service Provider of CdmFile */
public interface CdmFileProvider {

  /** The leading protocol string (without a trailing ":"). */
  String getProtocol();

  /** Determine if this Provider owns this DatasetUrl. */
  boolean isOwnerOf(DatasetUrl durl);

  /** Determine if this Provider owns this location. */
  default boolean isOwnerOf(String location) {
    return location.startsWith(getProtocol() + ":");
  }

  /** Open a location that this Provider is the owner of. */
  CdmFile open(String location, CancelTask cancelTask) throws IOException;
}
