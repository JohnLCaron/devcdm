/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.inventory;

import dev.ucdm.core.util.StringUtil2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

/** Use Java nio Paths */
public class MFileNio implements MFile {

  /**
   * Make MFile if file exists, otherwise return null
   *
   * @param filename full path name
   * @return MFile or null
   */
  public static MFileNio getExistingFile(String filename) throws IOException {
    if (filename == null)
      return null;
    Path path = Paths.get(filename);
    if (Files.exists(path))
      return new MFileNio(path);
    return null;
  }

  private final Path path;
  private final BasicFileAttributes attr;

  public MFileNio(Path path) throws IOException {
    this.path = path;
    this.attr = Files.readAttributes(path, BasicFileAttributes.class);
  }

  public MFileNio(Path path, BasicFileAttributes attr) {
    this.path = path;
    this.attr = attr;
  }

  public MFileNio(String filename) throws IOException {
    this.path = Paths.get(filename);
    this.attr = Files.readAttributes(path, BasicFileAttributes.class);
  }

  @Override
  public long getLastModified() {
    return attr.lastModifiedTime().toMillis();
  }

  @Override
  public long getLength() {
    return attr.size();
  }

  @Override
  public boolean isDirectory() {
    return attr.isDirectory();
  }

  @Override
  public boolean isReadable() {
    return Files.isReadable(path);
  }

  @Override
  public String getPath() {
    // no microsnot
    return StringUtil2.replace(path.toString(), '\\', "/");
  }

  @Override
  public String getShortName() {
    return path.getFileName().toString();
  }

  @Override
  public MFile getParent() throws IOException {
    return new MFileNio(path.getParent());
  }

  @Override
  public int compareTo(MFile o) {
    return getPath().compareTo(o.getPath());
  }

  @Override
  public String toString() {
    return getPath();
  }

  public Path getNioPath() {
    return path;
  }
}
