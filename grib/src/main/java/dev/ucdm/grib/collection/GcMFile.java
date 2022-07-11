/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.collection;

import dev.cdm.core.util.StringUtil2;

import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * MFile stored in GC index
 *
 * @author caron
 * @since 2/19/14
 */
public class GcMFile implements MFile {

  public static List<GcMFile> makeFiles(File directory, List<MFile> files, Set<Integer> allFileSet) {
    List<GcMFile> result = new ArrayList<>(files.size());
    String dirPath = StringUtil2.replace(directory.getPath(), '\\', "/");

    for (int index : allFileSet) {
      MFile file = files.get(index);
      String filename;
      if (file.getPath().startsWith(dirPath)) {
        filename = file.getPath().substring(dirPath.length());
        if (filename.startsWith("/"))
          filename = filename.substring(1);
      } else
        filename = file.getPath(); // when does this happen ??
      result.add(new GcMFile(directory, filename, file.getLastModified(), file.getLength(), index));
    }
    return result;
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  public final File directory;
  public final String shortName;
  public final long lastModified, length;
  public final int index;

  public GcMFile(File directory, String shortName, long lastModified, long length, int index) {
    this.directory = directory;
    this.shortName = shortName;
    this.lastModified = lastModified;
    this.index = index;
    this.length = length;
  }

  @Override
  public long getLastModified() {
    return lastModified;
  }

  @Override
  public long getLength() {
    return length;
  }

  @Override
  public boolean isDirectory() {
    return false;
  }

  @Override
  public String getPath() { // aka full name
    String path = new File(directory, shortName).getPath();
    return StringUtil2.replace(path, '\\', "/");
  }

  @Override
  public String getShortName() {
    return shortName;
  }

  @Override
  public MFile getParent() {
    return new MFileOS(directory);
  }

  @Override
  public int compareTo(MFile o) {
    return shortName.compareTo(o.getShortName());
  }

  @Override
  @Nullable
  public Object getAuxInfo() {
    return null;
  }

  @Override
  public void setAuxInfo(Object info) {}

  public File getDirectory() {
    return directory;
  }

  @Override
  public String toString() {
    return "GcMFile{" + "directory=" + directory + ", name='" + shortName + '\'' + ", lastModified=" + lastModified
        + ", length=" + length + ", index=" + index + '}';
  }
}
