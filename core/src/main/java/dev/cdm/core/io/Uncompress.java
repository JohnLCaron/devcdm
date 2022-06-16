package dev.cdm.core.io;

import dev.cdm.core.io.bzip2.CBZip2InputStream;
import dev.cdm.core.util.DiskCache;
import dev.cdm.core.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Uncompress {
  private static final Logger log = LoggerFactory.getLogger(Uncompress.class);

  public static String makeUncompressedFile(String filename) throws Exception {
    int pos = filename.lastIndexOf('.');
    String suffix = filename.substring(pos + 1);
    String uncompressedFilename = filename.substring(0, pos);

    File uncompressedFile = DiskCache.getFileStandardPolicy(uncompressedFilename);
    if (uncompressedFile.exists() && uncompressedFile.length() > 0) {
      // see if its locked - another thread is writing it
      FileLock lock = null;
      try (FileInputStream stream = new FileInputStream(uncompressedFile)) {
        // obtain the lock
        while (true) { // loop waiting for the lock
          try {
            lock = stream.getChannel().lock(0, 1, true); // wait till its unlocked
            break;
          } catch (OverlappingFileLockException oe) { // not sure why lock() doesnt block
            try {
              Thread.sleep(100); // msecs
            } catch (InterruptedException e1) {
              break;
            }
          }
        }

        return uncompressedFile.getPath();

      } finally {
        if (lock != null && lock.isValid()) {
          lock.release();
        }
      }
    }

    // ok gonna write it
    // make sure compressed file exists
    File file = new File(filename);
    if (!file.exists()) {
      return null; // bail out */
    }

    try (FileOutputStream fout = new FileOutputStream(uncompressedFile)) {
      // obtain the lock
      FileLock lock;
      while (true) { // loop waiting for the lock
        try {
          lock = fout.getChannel().lock(0, 1, false);
          break;

        } catch (OverlappingFileLockException oe) { // not sure why lock() doesnt block
          try {
            Thread.sleep(100); // msecs
          } catch (InterruptedException ignored) {
          }
        }
      }

      try {
        if (suffix.equalsIgnoreCase("Z")) {
          try (InputStream in = new UncompressInputStream(new FileInputStream(filename))) {
            IO.copyBuffered(in, fout, 100000);
          }

        } else if (suffix.equalsIgnoreCase("zip")) {

          try (ZipInputStream zin = new ZipInputStream(new FileInputStream(filename))) {
            ZipEntry ze = zin.getNextEntry();
            if (ze != null) {
              IO.copyBuffered(zin, fout, 100000);
            }
          }

        } else if (suffix.equalsIgnoreCase("bz2")) {
          try (InputStream in = new CBZip2InputStream(new FileInputStream(filename), true)) {
            IO.copyBuffered(in, fout, 100000);
          }

        } else if (suffix.equalsIgnoreCase("gzip") || suffix.equalsIgnoreCase("gz")) {
          try (InputStream in = new GZIPInputStream(new FileInputStream(filename))) {
            IO.copyBuffered(in, fout, 100000);
          }
        }
      } catch (Exception e) {
        // dont leave bad files around
        if (uncompressedFile.exists()) {
          if (!uncompressedFile.delete()) {
            log.warn("failed to delete uncompressed file (IOException) {}", uncompressedFile);
          }
        }
        throw e;

      } finally {
        if (lock != null && lock.isValid()) {
          lock.release();
        }
      }
    }

    return uncompressedFile.getPath();
  }
}
