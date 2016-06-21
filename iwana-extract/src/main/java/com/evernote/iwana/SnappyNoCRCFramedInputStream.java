/**
 * Copyright 2014,2015 Evernote Corporation.
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.iq80.snappy.Snappy;

/**
 * A snappy-compressed InputStream, using a variant of the Snappy Framing Format without
 * CRC values.
 * 
 * @see <a href="https://github.com/google/snappy/blob/master/framing_format.txt">Google's framing format</a>
 * @see <a href="https://github.com/obriensp/iWorkFileFormat/blob/master/Docs/index.md">obriensp's docs</a>
 */
public class SnappyNoCRCFramedInputStream extends InputStream {
  private byte[] header = new byte[4];
  private final byte[] readBuffer = new byte[128 * 1024];
  private final byte[] uncompressedBuffer = new byte[64 * 1024];
  private int readPointer = 0;
  private int filled = 0;
  private final InputStream in;
  private boolean eof = false;
  private boolean closeParent;

  /**
   * Creates a new {@link SnappyNoCRCFramedInputStream} wrapping the given
   * {@link InputStream}. Note that this closes the parent {@link InputStream}.
   * 
   * @param in The InputStream to wrap.
   */
  public SnappyNoCRCFramedInputStream(final InputStream in) {
    this(in, true);
  }

  /**
   * Creates a new {@link SnappyNoCRCFramedInputStream} wrapping the given
   * {@link InputStream}.
   * 
   * @param in The InputStream to wrap.
   * @param closeParent Whether a call to {@link #close()} should close the parent
   *          {@link InputStream}.
   */
  public SnappyNoCRCFramedInputStream(final InputStream in, final boolean closeParent) {
    this.in = in;
    this.closeParent = closeParent;
  }

  @Override
  public void close() throws IOException {
    if (closeParent) {
      in.close();
    }
  }

  private void checkFillBuffer() throws IOException {
    if (readPointer < filled) {
      return;
    }
    fillBuffer();
  }

  private void readFully(final byte[] buf, int toRead) throws IOException {
    int ptr = 0;
    int len = toRead;
    int read;
    while (len > 0 && ptr < toRead) {
      read = in.read(buf, ptr, len);
      if (read == -1) {
        eof = true;
        return;
      }
      ptr += read;
      len -= read;
    }
  }

  private void fillBuffer() throws IOException {
    FILL_LOOP : while (true) {

      if (eof) {
        throw new EOFException();
      }

      readPointer = 0;
      readFully(header, header.length);
      if (eof) {
        throw new EOFException();
      }

      int len =
          ((header[3] & 0xFF) << 16) | ((header[2] & 0xFF) << 8) | (header[1] & 0xFF);
      readFully(readBuffer, len);

      final int chunkType = header[0] & 0xFF;
      switch (chunkType) {
        case 0:
          // compressed
          if (len > readBuffer.length) {
            throw new IOException("Compressed chunk size exceeds buffer capacity: " + len
                + " > " + readBuffer.length);
          }

          filled = Snappy.uncompress(readBuffer, 0, len, uncompressedBuffer, 0);
          break FILL_LOOP;
        case 1:
          // uncompressed
          if (len > readBuffer.length) {
            throw new IOException("Uncompressed chunk size exceeds buffer capacity: "
                + len + " > " + readBuffer.length);
          }
          System.arraycopy(readBuffer, 0, uncompressedBuffer, 0, len);
          break FILL_LOOP;
        case 0xfe:
          // padding
          in.skip(len);
          break;
        case 0xff:
          // Stream identifier
          if (len != 6) {
            throw new IOException(
                "Stream identifier data should be exactly 6 bytes long, but was: " + len);
          }

          if (readBuffer[0] != 0x73 || readBuffer[1] != 0x4e || readBuffer[2] != 0x61
              || readBuffer[3] != 0x50 || readBuffer[4] != 0x70 || readBuffer[5] != 0x59) {
            throw new IOException("Could not find magic bytes in Stream identifier");
          }

          in.skip(len);
          break;
        default:
          if ((chunkType & 0x80) == 0) {
            // unskippable
            throw new IOException("Detected unskippable snappy chunk; type=" + chunkType
                + "; len=" + len);
          } else {
            // skippable
            in.skip(len);
          }
      }
    }
  }

  @Override
  public int available() throws IOException {
    if (eof) {
      return 0;
    }
    return filled - readPointer;
  }

  @Override
  public int read() throws IOException {
    try {
      checkFillBuffer();
    } catch (EOFException e) {
      eof = true;
      return -1;
    }

    if (readPointer > filled) {
      eof = true;
      return -1;
    }

    return uncompressedBuffer[readPointer++] & 0xFF;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if (len == 0) {
      return 0;
    }
    try {
      checkFillBuffer();
    } catch (EOFException e) {
      eof = true;
      return -1;
    }
    int read = Math.min(filled - readPointer, len);
    System.arraycopy(uncompressedBuffer, readPointer, b, off, read);

    readPointer += read;

    return read;
  }
}
