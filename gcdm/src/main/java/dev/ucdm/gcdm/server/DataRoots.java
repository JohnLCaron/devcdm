/* Copyright */
package dev.ucdm.gcdm.server;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataRoots {

  private static class PathComparator implements Comparator<String> {
    public int compare(String s1, String s2) {
      return s2.compareTo(s1); // reverse sort
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////
  private final TreeSet<String> treeSet = new TreeSet<>(new PathComparator()); // this should be in-memory for speed

  private Map<String, String> dataRoots = Map.of(
          "coreLocalDir/", "/home/snake/dev/github/devcdm/core/src/test/data/",
          "coreLocalNetcdf3Dir/", "/home/snake/dev/github/devcdm/core/src/test/data/netcdf3/",
          "coreLocalNetcdf4Dir/", "/home/snake/dev/github/devcdm/core/src/test/data/netcdf4/",
          "datasetLocalDir/", "/home/snake/dev/github/devcdm/dataset/src/test/data/",
          "gribLocalDir/", "/home/snake/dev/github/devcdm/grib/src/test/data/",
          "extraTestDir/", "/home/snake/tmp/testData/",
          "oldTestDir/",  "/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/"
  );

  // find the data path for a request starting with a dataRoot, else null
  @Nullable
  public String findDataPath(String request) {
    for (Map.Entry<String, String> dataRoot : dataRoots.entrySet()) {
      if (request.startsWith(dataRoot.getKey())) { // KISS
        return dataRoot.getValue();
      }
    }
    return null;
  }

  private record KeyValue(String key, String value) {}

  // find the root for a data path starting with a dataRoot, else null. if more than one, use longest match
  @Nullable
  public String findDataRoot(String path) {
    KeyValue found = findDataRootEntry(path);
    return found == null ? null : found.key();
  }

  @Nullable
  private KeyValue findDataRootEntry(String path) {
    path = path.replace("\\", "/");
    File file = new File(path);
    String canonical = null;
    try {
      canonical = file.getCanonicalPath();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    int foundLen = 0;
    Map.Entry<String, String> found = null;
    for (Map.Entry<String, String> dataRoot : dataRoots.entrySet()) {
      if (canonical.startsWith(dataRoot.getValue())) {
        if (dataRoot.getValue().length() > foundLen) {
          foundLen = dataRoot.getValue().length();
          found = dataRoot;
        }
      }
    }
    return found == null ? null : new KeyValue(found.getKey(), canonical.substring(foundLen));
  }

  // convert the request to a data path starting with a dataRoot, else null
  @Nullable
  public String convertRootToPath(String request) {
    for (Map.Entry<String, String> dataRoot : dataRoots.entrySet()) {
      if (request.startsWith(dataRoot.getKey())) { // KISS
        return dataRoot.getValue() + request.substring(dataRoot.getKey().length());
      }
    }
    return null;
  }

  // convert the data path to a request starting with a dataRoot, else null. if more than one, use longest match
  @Nullable
  public String convertPathToRoot(String path) {
    KeyValue found = findDataRootEntry(path);
    return found == null ? null : found.key() + found.value();
  }

  public String makeGcdmUrl(String filename) throws IOException {
    filename = filename.replace("\\", "/");
    File file = new File(filename);
    String path = convertPathToRoot(file.getCanonicalPath());
    if (path == null) {
      throw new IllegalArgumentException("No DataRoot found for " + filename);
    }
    String gcdmUrl = "gcdm://localhost:16111/" + path;
    return gcdmUrl;
  }

  /**
   * Find the longest path match.
   * 
   * @param reqPath find object with longest match where reqPath.startsWith( key)
   * @return the value whose key is the longest that matches path, or null if none
   */
  @Nullable
  public String getDataRootOld(String reqPath) {
    SortedSet<String> tail = treeSet.tailSet(reqPath);
    if (tail.isEmpty())
      return null;
    String after = tail.first();
    if (reqPath.startsWith(after)) // common case
      return tail.first();

    // have to check more, until no common starting chars
    for (String key : tail) {
      if (reqPath.startsWith(key))
        return key;

      // terminate when there's no match at all.
      if (countMatches(reqPath, key) == 0)
        break;
    }
    return null;
  }

  /**
   * Count number of chars that match in two strings, starting from front.
   *
   * @param s1 compare this string
   * @param s2 compare this string
   * @return number of matching chars, starting from first char
   */
  private int countMatches(String s1, String s2) {
    int i = 0;
    while ((i < s1.length()) && (i < s2.length())) {
      if (s1.charAt(i) != s2.charAt(i)) {
        break;
      }
      i++;
    }
    return i;
  }

}
