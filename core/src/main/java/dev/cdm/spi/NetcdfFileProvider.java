package dev.cdm.spi;

import dev.cdm.core.NetcdfFile;

import java.io.IOException;

/**
 * A Service Provider of NetcdfFile, used by remote protocols (dods, cdmremote)
 * or anything not reading from a RandomAccessFile.
 */
public interface NetcdfFileProvider {

  /** The leading protocol string (without a trailing ":"). */
  String getProtocol();

  /** Determine if this Provider owns this DatasetUrl. */
  // boolean isOwnerOf(DatasetUrl durl);

  /** Determine if this Provider owns this location. */
  default boolean isOwnerOf(String location) {
    return location.startsWith(getProtocol() + ":");
  }

  /** Open a location that this Provider is the owner of. */
  NetcdfFile open(String location, dev.cdm.util.CancelTask cancelTask) throws IOException;
}
