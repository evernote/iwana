/**
 * Copyright 2014-2016 Evernote Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evernote.iwana;

import java.io.IOException;
import java.io.InputStream;

/**
 * A wrapping {@link InputStream} that allows a maximum number of bytes to be read.
 * 
 * Any further attempts to read bytes will behave as if the end of stream was reached.
 */
public class RestrictedSizeInputStream extends InputStream {
  private final InputStream in;

  private long numReadable;
  private long numRead = 0;

  /**
   * Wraps a {@link RestrictedSizeInputStream} around the given {@link InputStream}, with
   * an initial limit of <em>0</em> bytes allowed for reading from the stream.
   * 
   * Use {@link #setNumBytesReadable(long)} and {@link #addNumBytesReadable(long)} to
   * programmatically change the limit of readable bytes. Note that the initial limit of
   * readable bytes is 0.
   * 
   * @param in The {@link InputStream} to wrap.
   */
  public RestrictedSizeInputStream(final InputStream in) {
    this(in, 0);
  }

  /**
   * Wraps a {@link RestrictedSizeInputStream} around the given {@link InputStream}, with
   * an initial limit for the number of bytes allowed for reading from the stream.
   * 
   * Use {@link #setNumBytesReadable(long)} and {@link #addNumBytesReadable(long)} to
   * programmatically change the limit of readable bytes.
   * 
   * @param in The {@link InputStream} to wrap.
   * @param maxSize The initial limit of readable bytes.
   */
  public RestrictedSizeInputStream(final InputStream in, final long maxSize) {
    this.in = in;
    this.numReadable = maxSize;
  }

  @Override
  public int read() throws IOException {
    if (numReadable <= 0) {
      return -1;
    }

    int r = in.read();
    if (r >= 0) {
      numReadable--;
      numRead++;
    }
    return r;
  }

  @Override
  public int available() throws IOException {
    int a = in.available();
    if (a > numReadable) {
      if (numReadable <= 0) {
        return 0;
      }
      return (int) numReadable;
    } else {
      return a;
    }
  }

  /**
   * Returns the overall number of bytes read.
   * 
   * @return The number of bytes read.
   */
  public long getNumBytesRead() {
    return numRead;
  }

  /**
   * Returns the maximum number of bytes currently permitted to read from the stream
   * before limits are enforced.
   * 
   * This number may be higher than the actual number of bytes readable from the stream.
   * 
   * @return The number of bytes permitted to read.
   */
  public long getNumBytesReadable() {
    return numReadable;
  }

  /**
   * Sets the maximum number of bytes currently permitted to read from the stream before
   * limits are enforced.
   * 
   * This number may be higher than the actual number of bytes readable from the stream.
   * 
   * @param numBytes The number of bytes permitted to read.
   */
  public void setNumBytesReadable(long numBytes) {
    this.numReadable = numBytes;
  }

  /**
   * Adds to the maximum number of bytes currently permitted to read from the stream
   * before limits are enforced.
   * 
   * @param numBytes Additional number of bytes.
   */
  public void addNumBytesReadable(long numBytes) {
    this.numReadable += numReadable;
  }

  @Override
  public long skip(long n) throws IOException {
    if (n <= 0) {
      return 0;
    }

    if (numReadable <= 0) {
      return 0;
    }

    if (n > numReadable) {
      n = (int) numReadable;
    }

    long r = in.skip(n);

    numRead += r;
    numReadable -= r;

    return r;
  }

  /**
   * Skips as many bytes as there are still readable.
   * 
   * This is equivalent to <code>skip(getNumBytesReadable())</code>.
   */
  public void skipRest() throws IOException {
    skip(numReadable);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if (numReadable <= 0) {
      return -1;
    }

    if (len == 0) {
      return 0;
    }

    if (len > numReadable) {
      len = (int) numReadable;
    }

    int r = in.read(b, off, len);
    if (r >= 0) {
      numReadable -= r;
      numRead += r;
    }
    return r;
  }
}
