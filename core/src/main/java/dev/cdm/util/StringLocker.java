/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A list of strings that only allows one thread to use any given value at the same time.
 *
 * @author cmrose
 */
public class StringLocker {
  private final List<String> stringList = Collections.synchronizedList(new ArrayList<>());
  private boolean waiting;

  public synchronized void control(String item) {
    // If the string is in use by another thread then wait() for the other thread
    waiting = stringList.contains(item);
    while (waiting) {
      try {
        wait();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    // Finished waiting so the thread can have the string
    stringList.add(item);
  }

  public synchronized void release(String item) {
    // Tell StringLocker the thread is done with the string
    stringList.remove(item);
    waiting = false;
    notifyAll();
  }

  public String toString() {
    return stringList.toString();
  }

}
