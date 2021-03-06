/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Similar to a DataInputStream that keeps track of position.
 * The position must always increase, no going backwards.
 * Note cant handle byte order yet - assume big endian(?).
 */
public class PositioningDataInputStream {
  private final DataInputStream delegate;
  private long cpos;

  public PositioningDataInputStream(InputStream is) {
    if (is instanceof DataInputStream)
      delegate = (DataInputStream) is;
    else
      delegate = new DataInputStream(is);
  }

  private void seek(long pos) throws IOException {
    if (pos < cpos)
      throw new IllegalArgumentException("Cannot go backwards; current=" + cpos + " request=" + pos);
    long want = pos - cpos;
    while (want > 0)
      want -= delegate.skip(want);
    cpos = pos;
  }

  public void read(long pos, byte[] dest, int off, int len) throws IOException {
    seek(pos);
    delegate.readFully(dest, off, len);
    cpos += len;
  }

  public void readShort(long pos, short[] dest, int off, int len) throws IOException {
    seek(pos);
    for (int i = 0; i < len; i++)
      dest[off + i] = delegate.readShort();
    cpos += len * 2;
  }

  public void readInt(long pos, int[] dest, int off, int len) throws IOException {
    seek(pos);
    for (int i = 0; i < len; i++)
      dest[off + i] = delegate.readInt();
    cpos += len * 4;
  }

  public void readLong(long pos, long[] dest, int off, int len) throws IOException {
    seek(pos);
    for (int i = 0; i < len; i++)
      dest[off + i] = delegate.readLong();
    cpos += len * 8;
  }

  public void readFloat(long pos, float[] dest, int off, int len) throws IOException {
    seek(pos);
    for (int i = 0; i < len; i++)
      dest[off + i] = delegate.readFloat();
    cpos += len * 4;
  }

  public void readDouble(long pos, double[] dest, int off, int len) throws IOException {
    seek(pos);
    for (int i = 0; i < len; i++)
      dest[off + i] = delegate.readDouble();
    cpos += len * 8;
  }
}
